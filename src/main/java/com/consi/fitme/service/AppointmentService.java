package com.consi.fitme.service;

import com.consi.fitme.dto.AppointmentDTO;
import com.consi.fitme.dto.request.BookAppointmentRequestDTO;
import com.consi.fitme.dto.request.UpdateAppointmentRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.appointment.AppointmentCancelWindowExpiredException;
import com.consi.fitme.exception.appointment.AppointmentNotAvailableException;
import com.consi.fitme.exception.appointment.AppointmentNotBookedException;
import com.consi.fitme.exception.appointment.AppointmentNotFoundException;
import com.consi.fitme.exception.appointment.AppointmentOwnershipException;
import com.consi.fitme.exception.appointment.AppointmentUserRequiredException;
import com.consi.fitme.exception.appointment.MembershipExpiredException;
import com.consi.fitme.exception.appointment.NoRemainingAppointmentsException;
import com.consi.fitme.exception.user.UserNotFoundException;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.model.entity.Pilates;
import com.consi.fitme.model.entity.Termin;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.AppointmentRepository;
import com.consi.fitme.repository.PilatesRepository;
import com.consi.fitme.repository.TerminRepository;
import com.consi.fitme.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

  private static final String ENTITY_TYPE = "APPOINTMENT";
  private static final String ROLE_ADMIN = "ROLE_ADMIN";
  private static final int CANCEL_WINDOW_HOURS = 12;
  private static final int MEMBERSHIP_DURATION_DAYS = 35;

  private final AppointmentRepository repository;
  private final TerminRepository terminRepository;
  private final PilatesRepository pilatesRepository;
  private final UserRepository userRepository;
  private final AuditLogService auditLogService;

  public List<AppointmentDTO> getAllAppointments() {
    return enrich(repository.findAll());
  }

  public AppointmentDTO getAppointment(Long id) {
    return toDto(findOrThrow(id));
  }

  public List<AppointmentDTO> getAvailableAppointments(LocalDate dateFilter) {
    List<Appointment> available = repository.findAllByStatus(AppointmentStatus.AVAILABLE);
    List<Appointment> bookable = filterToActiveTerminAndPilates(available);

    if (dateFilter == null) {
      return enrich(bookable);
    }

    List<Long> terminIdsForDate =
        terminRepository.findByDateAndStatus(dateFilter, Status.ACTIVE).stream()
            .map(Termin::getId)
            .toList();

    return enrich(
        bookable.stream().filter(a -> terminIdsForDate.contains(a.getTerminId())).toList());
  }

  public List<AppointmentDTO> getByUserId(Long userId) {
    if (!isAdmin() && !resolveCurrentUser().getId().equals(userId)) {
      throw new AppointmentOwnershipException();
    }

    return enrich(repository.findAllByUserId(userId));
  }

  @Transactional
  public AppointmentDTO bookAppointment(BookAppointmentRequestDTO bookAppointmentRequestDTO) {
    boolean isAdmin = isAdmin();
    User currentUser = resolveCurrentUser();

    Appointment appointment = findOrThrow(bookAppointmentRequestDTO.getAppointmentId());
    ensureSlotBookable(appointment);

    Long targetUserId;
    if (isAdmin) {
      if (bookAppointmentRequestDTO.getUserId() == null) {
        throw new AppointmentUserRequiredException();
      }
      targetUserId = bookAppointmentRequestDTO.getUserId();
    } else {
      targetUserId = currentUser.getId();
    }

    User targetUser =
        userRepository
            .findById(targetUserId)
            .orElseThrow(() -> new UserNotFoundException(targetUserId));

    if (!isAdmin) {
      if (targetUser.getMembershipExpiresAt() != null
          && targetUser.getMembershipExpiresAt().isBefore(LocalDate.now())) {
        throw new MembershipExpiredException();
      }
      Integer remaining = targetUser.getRemainingAppointments();
      if (remaining == null || remaining <= 0) {
        throw new NoRemainingAppointmentsException();
      }
    }

    appointment.setStatus(AppointmentStatus.BOOKED);
    appointment.setUserId(targetUserId);
    Appointment saved = claimSlot(appointment);

    if (targetUser.getMembershipExpiresAt() == null) {
      targetUser.setMembershipExpiresAt(LocalDate.now().plusDays(MEMBERSHIP_DURATION_DAYS));
    }
    if (!isAdmin) {
      targetUser.setRemainingAppointments(targetUser.getRemainingAppointments() - 1);
    }
    userRepository.save(targetUser);

    AppointmentDTO dto = toDto(saved);
    auditLogService.logCreate(ENTITY_TYPE, saved.getId(), dto);
    return dto;
  }

  @Transactional
  public AppointmentDTO updateAppointment(
      Long id, UpdateAppointmentRequestDTO updateAppointmentRequestDTO) {
    boolean isAdmin = isAdmin();
    User currentUser = resolveCurrentUser();

    Appointment appointment = findOrThrow(id);
    if (appointment.getStatus() != AppointmentStatus.BOOKED) {
      throw new AppointmentNotBookedException();
    }

    if (!isAdmin) {
      if (!currentUser.getId().equals(appointment.getUserId())) {
        throw new AppointmentOwnershipException();
      }
      ensureWithinCancelWindow(appointment);
    }

    AppointmentDTO oldState = toDto(appointment);

    if (updateAppointmentRequestDTO.getTargetAppointmentId() == null) {
      appointment.setStatus(AppointmentStatus.AVAILABLE);
      appointment.setUserId(null);
      Appointment saved = repository.save(appointment);

      AppointmentDTO newState = toDto(saved);
      auditLogService.logUpdate(ENTITY_TYPE, saved.getId(), oldState, newState);
      return newState;
    }

    return reschedule(appointment, updateAppointmentRequestDTO.getTargetAppointmentId(), oldState);
  }

  @Transactional
  public MessageResponseDTO deleteAppointment(Long id) {
    Appointment appointment = findOrThrow(id);
    AppointmentDTO oldState = toDto(appointment);
    repository.delete(appointment);
    auditLogService.logDelete(ENTITY_TYPE, id, oldState);
    return new MessageResponseDTO("Uspešno obrisan appointment za ID: " + id);
  }

  private AppointmentDTO reschedule(
      Appointment sourceAppointment, Long targetAppointmentId, AppointmentDTO sourceOldState) {
    Appointment targetAppointment = findOrThrow(targetAppointmentId);
    ensureSlotBookable(targetAppointment);
    AppointmentDTO targetOldState = toDto(targetAppointment);

    Long bookedUserId = sourceAppointment.getUserId();

    sourceAppointment.setStatus(AppointmentStatus.AVAILABLE);
    sourceAppointment.setUserId(null);
    Appointment savedSource = repository.save(sourceAppointment);

    targetAppointment.setStatus(AppointmentStatus.BOOKED);
    targetAppointment.setUserId(bookedUserId);
    Appointment savedTarget = claimSlot(targetAppointment);

    auditLogService.logUpdate(ENTITY_TYPE, savedSource.getId(), sourceOldState, toDto(savedSource));
    AppointmentDTO targetNewState = toDto(savedTarget);
    auditLogService.logUpdate(ENTITY_TYPE, savedTarget.getId(), targetOldState, targetNewState);
    return targetNewState;
  }

  private void ensureSlotBookable(Appointment appointment) {
    if (appointment.getStatus() != AppointmentStatus.AVAILABLE) {
      throw new AppointmentNotAvailableException();
    }
    Termin termin = terminRepository.findById(appointment.getTerminId()).orElse(null);
    Pilates pilates = pilatesRepository.findById(appointment.getPilatesId()).orElse(null);
    if (termin == null
        || termin.getStatus() != Status.ACTIVE
        || pilates == null
        || pilates.getStatus() != Status.ACTIVE) {
      throw new AppointmentNotAvailableException();
    }
  }

  private Appointment claimSlot(Appointment appointment) {
    try {
      return repository.saveAndFlush(appointment);
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new AppointmentNotAvailableException();
    }
  }

  private List<Appointment> filterToActiveTerminAndPilates(List<Appointment> appointments) {
    Map<Long, Termin> terminById =
        terminRepository
            .findAllById(appointments.stream().map(Appointment::getTerminId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Termin::getId, Function.identity()));
    Map<Long, Pilates> pilatesById =
        pilatesRepository
            .findAllById(appointments.stream().map(Appointment::getPilatesId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Pilates::getId, Function.identity()));

    return appointments.stream()
        .filter(
            a -> {
              Termin termin = terminById.get(a.getTerminId());
              Pilates pilates = pilatesById.get(a.getPilatesId());
              return termin != null
                  && termin.getStatus() == Status.ACTIVE
                  && pilates != null
                  && pilates.getStatus() == Status.ACTIVE;
            })
        .toList();
  }

  private void ensureWithinCancelWindow(Appointment appointment) {
    Long appointmentId = appointment.getId();
    Termin termin =
        terminRepository
            .findById(appointment.getTerminId())
            .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

    LocalDateTime cutoff =
        LocalDateTime.of(termin.getDate(), termin.getStartTime()).minusHours(CANCEL_WINDOW_HOURS);
    if (LocalDateTime.now().isAfter(cutoff)) {
      throw new AppointmentCancelWindowExpiredException();
    }
  }

  private Appointment findOrThrow(Long id) {
    return repository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
  }

  private boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
  }

  private User resolveCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
      throw new IllegalStateException("Nema autentikovanog korisnika u SecurityContext-u");
    }
    return currentUser;
  }

  private List<AppointmentDTO> enrich(List<Appointment> appointments) {
    List<Long> terminIds = appointments.stream().map(Appointment::getTerminId).distinct().toList();
    List<Long> pilatesIds =
        appointments.stream().map(Appointment::getPilatesId).distinct().toList();

    Map<Long, Termin> terminById =
        terminRepository.findAllById(terminIds).stream()
            .collect(Collectors.toMap(Termin::getId, Function.identity()));
    Map<Long, Pilates> pilatesById =
        pilatesRepository.findAllById(pilatesIds).stream()
            .collect(Collectors.toMap(Pilates::getId, Function.identity()));

    return appointments.stream()
        .map(a -> toDto(a, terminById.get(a.getTerminId()), pilatesById.get(a.getPilatesId())))
        .toList();
  }

  private AppointmentDTO toDto(Appointment appointment) {
    Termin termin = terminRepository.findById(appointment.getTerminId()).orElse(null);
    Pilates pilates = pilatesRepository.findById(appointment.getPilatesId()).orElse(null);
    return toDto(appointment, termin, pilates);
  }

  private AppointmentDTO toDto(Appointment appointment, Termin termin, Pilates pilates) {
    return AppointmentDTO.builder()
        .id(appointment.getId())
        .terminId(appointment.getTerminId())
        .pilatesId(appointment.getPilatesId())
        .userId(appointment.getUserId())
        .status(appointment.getStatus())
        .terminDate(termin != null ? termin.getDate() : null)
        .terminStartTime(termin != null ? termin.getStartTime() : null)
        .terminEndTime(termin != null ? termin.getEndTime() : null)
        .pilatesPosition(pilates != null ? pilates.getPosition() : null)
        .pilatesName(pilates != null ? pilates.getName() : null)
        .build();
  }
}
