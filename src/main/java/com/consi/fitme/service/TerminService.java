package com.consi.fitme.service;

import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.termin.InvalidTerminTimeRangeException;
import com.consi.fitme.exception.termin.TerminDeleteBlockedException;
import com.consi.fitme.exception.termin.TerminNotFoundException;
import com.consi.fitme.exception.termin.TerminOverlapException;
import com.consi.fitme.mapper.TerminPatchMapper;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Termin;
import com.consi.fitme.model.entity.TerminTemplate;
import com.consi.fitme.repository.AppointmentRepository;
import com.consi.fitme.repository.TerminRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TerminService {

  private final TerminRepository repository;
  private final TerminPatchMapper patchMapper;
  private final AppointmentGenerationService appointmentGenerationService;
  private final AppointmentRepository appointmentRepository;

  public List<TerminDTO> getAllTermini() {
    return repository.findAllByStatusNot(Status.DELETED).stream().map(this::toDto).toList();
  }

  public TerminDTO getTermin(Long id) {
    return toDto(findActiveOrInactiveById(id));
  }

  @Transactional
  public TerminDTO createTermin(CreateTerminRequestDTO createTerminRequestDTO) {
    LocalDate date = createTerminRequestDTO.getDate();
    LocalTime startTime = createTerminRequestDTO.getStartTime();
    LocalTime endTime = createTerminRequestDTO.getEndTime();

    ensureValidTimeRange(startTime, endTime);
    ensureNoOverlap(date, startTime, endTime, null);

    Termin termin = Termin.builder().date(date).startTime(startTime).endTime(endTime).build();
    Termin savedTermin = repository.save(termin);

    if (savedTermin.getStatus() == Status.ACTIVE) {
      appointmentGenerationService.generateForTermin(savedTermin.getId());
    }

    return toDto(savedTermin);
  }

  @Transactional
  public TerminDTO updateTermin(Long id, UpdateTerminRequestDTO updateTerminRequestDTO) {
    Termin existingTermin = findActiveOrInactiveById(id);

    LocalDate nextDate =
        updateTerminRequestDTO.getDate() != null
            ? updateTerminRequestDTO.getDate()
            : existingTermin.getDate();
    LocalTime nextStartTime =
        updateTerminRequestDTO.getStartTime() != null
            ? updateTerminRequestDTO.getStartTime()
            : existingTermin.getStartTime();
    LocalTime nextEndTime =
        updateTerminRequestDTO.getEndTime() != null
            ? updateTerminRequestDTO.getEndTime()
            : existingTermin.getEndTime();

    ensureValidTimeRange(nextStartTime, nextEndTime);
    ensureNoOverlap(nextDate, nextStartTime, nextEndTime, id);

    patchMapper.applyPatch(updateTerminRequestDTO, existingTermin);
    return toDto(repository.save(existingTermin));
  }

  // Namerno bez @Transactional: poziva se samo iz TerminGenerationService.generateForTemplate
  // (koji jeste transakcioni); sopstveni tx proxy bi na TerminOverlapException markirao
  // zajedničku transakciju rollback-only pre nego što je pozivalac uhvati i preskoči datum.
  Termin createTerminFromTemplate(TerminTemplate template, LocalDate date) {
    ensureValidTimeRange(template.getStartTime(), template.getEndTime());
    ensureNoOverlap(date, template.getStartTime(), template.getEndTime(), null);

    Termin termin =
        Termin.builder()
            .date(date)
            .startTime(template.getStartTime())
            .endTime(template.getEndTime())
            .templateId(template.getId())
            .build();
    return repository.save(termin);
  }

  @Transactional
  public MessageResponseDTO deleteTermin(Long id) {
    Termin termin = findActiveOrInactiveById(id);

    if (appointmentRepository.existsByTerminIdAndStatus(id, AppointmentStatus.BOOKED)) {
      throw new TerminDeleteBlockedException(id);
    }

    termin.setStatus(Status.DELETED);
    repository.save(termin);
    return new MessageResponseDTO("Uspešno obrisan termin za ID: " + id);
  }

  private void ensureValidTimeRange(LocalTime startTime, LocalTime endTime) {
    if (!endTime.isAfter(startTime)) {
      throw new InvalidTerminTimeRangeException();
    }
  }

  private void ensureNoOverlap(
      LocalDate date, LocalTime startTime, LocalTime endTime, Long currentTerminId) {
    boolean overlaps =
        repository.findByDateAndStatus(date, Status.ACTIVE).stream()
            .filter(other -> currentTerminId == null || !other.getId().equals(currentTerminId))
            .anyMatch(
                other ->
                    startTime.isBefore(other.getEndTime())
                        && other.getStartTime().isBefore(endTime));

    if (overlaps) {
      throw new TerminOverlapException();
    }
  }

  private Termin findActiveOrInactiveById(Long id) {
    return repository
        .findByIdAndStatusNot(id, Status.DELETED)
        .orElseThrow(() -> new TerminNotFoundException(id));
  }

  private TerminDTO toDto(Termin termin) {
    return TerminDTO.builder()
        .id(termin.getId())
        .date(termin.getDate())
        .startTime(termin.getStartTime())
        .endTime(termin.getEndTime())
        .status(termin.getStatus())
        .build();
  }
}
