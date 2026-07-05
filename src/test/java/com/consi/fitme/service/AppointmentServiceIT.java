package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.AppointmentDTO;
import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.AppointmentSearchRequestDTO;
import com.consi.fitme.dto.request.BookAppointmentRequestDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateAppointmentRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.exception.appointment.AppointmentCancelWindowExpiredException;
import com.consi.fitme.exception.appointment.AppointmentNotAvailableException;
import com.consi.fitme.exception.appointment.AppointmentNotBookedException;
import com.consi.fitme.exception.appointment.AppointmentOwnershipException;
import com.consi.fitme.exception.appointment.AppointmentUserRequiredException;
import com.consi.fitme.exception.appointment.MembershipExpiredException;
import com.consi.fitme.exception.appointment.NoRemainingAppointmentsException;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.AppointmentRepository;
import com.consi.fitme.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class AppointmentServiceIT {

  @Autowired private AppointmentService service;
  @Autowired private UserService userService;
  @Autowired private TerminService terminService;
  @Autowired private PilatesService pilatesService;
  @Autowired private UserRepository userRepository;
  @Autowired private AppointmentRepository appointmentRepository;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void givenClientWithCredit_whenBookAvailableAppointment_thenBooksAndDecrementsCredit() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");

    AppointmentDTO booked =
        service.bookAppointment(
            BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    assertThat(booked.getUserId()).isEqualTo(client.getId());
    assertThat(booked.getUserFullName()).isEqualTo(client.getFullName());

    User persistedClient = userRepository.findById(client.getId()).orElseThrow();
    assertThat(persistedClient.getRemainingAppointments()).isEqualTo(2);
  }

  @Test
  void givenClientWithoutCredit_whenBookAppointment_thenThrowsNoRemainingAppointmentsException() {
    UserDTO client = createActiveClient(seed(), 0);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    BookAppointmentRequestDTO bookRequest =
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build();

    assertThatThrownBy(() -> service.bookAppointment(bookRequest))
        .isInstanceOf(NoRemainingAppointmentsException.class);
  }

  @Test
  void givenClientWithNoPriorBooking_whenBookAppointment_thenSetsMembershipExpiresAt35DaysOut() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");

    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    User persistedClient = userRepository.findById(client.getId()).orElseThrow();
    assertThat(persistedClient.getMembershipExpiresAt()).isEqualTo(LocalDate.now().plusDays(35));
  }

  @Test
  void
      givenClientWithActiveMembership_whenBookAnotherAppointment_thenMembershipExpiresAtUnchanged() {
    UserDTO client = createActiveClient(seed(), 3);
    Long firstAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(firstAppointmentId).build());
    LocalDate expiryAfterFirstBooking =
        userRepository.findById(client.getId()).orElseThrow().getMembershipExpiresAt();

    Long secondAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(11, 0), LocalTime.of(12, 0));
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(secondAppointmentId).build());

    User persistedClient = userRepository.findById(client.getId()).orElseThrow();
    assertThat(persistedClient.getMembershipExpiresAt()).isEqualTo(expiryAfterFirstBooking);
  }

  @Test
  void givenClientWithExpiredMembership_whenBookAppointment_thenThrowsMembershipExpiredException() {
    UserDTO client = createActiveClient(seed(), 3);
    User user = userRepository.findById(client.getId()).orElseThrow();
    user.setMembershipExpiresAt(LocalDate.now().minusDays(1));
    userRepository.save(user);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    BookAppointmentRequestDTO bookRequest =
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build();

    assertThatThrownBy(() -> service.bookAppointment(bookRequest))
        .isInstanceOf(MembershipExpiredException.class);
  }

  @Test
  void givenAdminBookingForClientWithExpiredMembership_whenBookAppointment_thenSucceeds() {
    UserDTO admin = createActiveAdmin(seed());
    UserDTO client = createActiveClient(seed(), 3);
    User user = userRepository.findById(client.getId()).orElseThrow();
    user.setMembershipExpiresAt(LocalDate.now().minusDays(1));
    userRepository.save(user);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(admin.getId(), "ADMIN");

    AppointmentDTO booked =
        service.bookAppointment(
            BookAppointmentRequestDTO.builder()
                .appointmentId(appointmentId)
                .userId(client.getId())
                .build());

    assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
  }

  @Test
  void givenAlreadyBookedAppointment_whenBookAgain_thenThrowsAppointmentNotAvailableException() {
    UserDTO firstClient = createActiveClient(seed(), 3);
    UserDTO secondClient = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));

    authenticateAs(firstClient.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    authenticateAs(secondClient.getId(), "CLIENT");
    BookAppointmentRequestDTO bookRequest =
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build();
    assertThatThrownBy(() -> service.bookAppointment(bookRequest))
        .isInstanceOf(AppointmentNotAvailableException.class);
  }

  @Test
  void
      givenAdminBookingWithoutUserId_whenBookAppointment_thenThrowsAppointmentUserRequiredException() {
    UserDTO admin = createActiveAdmin(seed());
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(admin.getId(), "ADMIN");
    BookAppointmentRequestDTO bookRequest =
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build();

    assertThatThrownBy(() -> service.bookAppointment(bookRequest))
        .isInstanceOf(AppointmentUserRequiredException.class);
  }

  @Test
  void
      givenAdminBookingForClientWithoutCredit_whenBookAppointment_thenBooksWithoutTouchingCredit() {
    UserDTO admin = createActiveAdmin(seed());
    UserDTO client = createActiveClient(seed(), 0);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(admin.getId(), "ADMIN");

    AppointmentDTO booked =
        service.bookAppointment(
            BookAppointmentRequestDTO.builder()
                .appointmentId(appointmentId)
                .userId(client.getId())
                .build());

    assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    assertThat(booked.getUserId()).isEqualTo(client.getId());

    User persistedClient = userRepository.findById(client.getId()).orElseThrow();
    assertThat(persistedClient.getRemainingAppointments()).isZero();
  }

  @Test
  void
      givenBookedAppointmentFarInFuture_whenClientCancels_thenReleasesSlotWithoutRefundingCredit() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    AppointmentDTO canceled =
        service.updateAppointment(appointmentId, UpdateAppointmentRequestDTO.builder().build());

    assertThat(canceled.getStatus()).isEqualTo(AppointmentStatus.AVAILABLE);
    assertThat(canceled.getUserId()).isNull();

    User persistedClient = userRepository.findById(client.getId()).orElseThrow();
    assertThat(persistedClient.getRemainingAppointments()).isEqualTo(2);
  }

  @Test
  void
      givenBookedAppointmentLessThan12hAway_whenClientCancels_thenThrowsAppointmentCancelWindowExpiredException() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId = createAppointmentForCancelWindowTest(LocalDateTime.now().plusMinutes(5));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());
    UpdateAppointmentRequestDTO cancelRequest = UpdateAppointmentRequestDTO.builder().build();

    assertThatThrownBy(() -> service.updateAppointment(appointmentId, cancelRequest))
        .isInstanceOf(AppointmentCancelWindowExpiredException.class);
  }

  @Test
  void
      givenBookedAppointmentExactly12hAway_whenClientCancels_thenThrowsAppointmentCancelWindowExpiredException() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId = createAppointmentForCancelWindowTest(LocalDateTime.now().plusHours(12));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());
    UpdateAppointmentRequestDTO cancelRequest = UpdateAppointmentRequestDTO.builder().build();

    assertThatThrownBy(() -> service.updateAppointment(appointmentId, cancelRequest))
        .isInstanceOf(AppointmentCancelWindowExpiredException.class);
  }

  @Test
  void
      givenBookedAppointment11h59mAway_whenClientCancels_thenThrowsAppointmentCancelWindowExpiredException() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointmentForCancelWindowTest(LocalDateTime.now().plusHours(11).plusMinutes(59));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());
    UpdateAppointmentRequestDTO cancelRequest = UpdateAppointmentRequestDTO.builder().build();

    assertThatThrownBy(() -> service.updateAppointment(appointmentId, cancelRequest))
        .isInstanceOf(AppointmentCancelWindowExpiredException.class);
  }

  @Test
  void givenBookedAppointment12h01mAway_whenClientCancels_thenSucceeds() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointmentForCancelWindowTest(LocalDateTime.now().plusHours(12).plusMinutes(1));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    AppointmentDTO canceled =
        service.updateAppointment(appointmentId, UpdateAppointmentRequestDTO.builder().build());

    assertThat(canceled.getStatus()).isEqualTo(AppointmentStatus.AVAILABLE);
  }

  @Test
  void givenBookedAppointment_whenAnotherClientCancels_thenThrowsAppointmentOwnershipException() {
    UserDTO owner = createActiveClient(seed(), 3);
    UserDTO otherClient = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));

    authenticateAs(owner.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    authenticateAs(otherClient.getId(), "CLIENT");
    UpdateAppointmentRequestDTO cancelRequest = UpdateAppointmentRequestDTO.builder().build();
    assertThatThrownBy(() -> service.updateAppointment(appointmentId, cancelRequest))
        .isInstanceOf(AppointmentOwnershipException.class);
  }

  @Test
  void givenAvailableAppointment_whenCancelAttempted_thenThrowsAppointmentNotBookedException() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    UpdateAppointmentRequestDTO cancelRequest = UpdateAppointmentRequestDTO.builder().build();

    assertThatThrownBy(() -> service.updateAppointment(appointmentId, cancelRequest))
        .isInstanceOf(AppointmentNotBookedException.class);
  }

  @Test
  void
      givenBookedAppointment_whenClientReschedulesToAvailableSlot_thenSwapsSlotsWithoutConsumingCredit() {
    UserDTO client = createActiveClient(seed(), 3);
    Long originalAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    Long targetAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(11, 0), LocalTime.of(12, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(originalAppointmentId).build());

    AppointmentDTO rescheduled =
        service.updateAppointment(
            originalAppointmentId,
            UpdateAppointmentRequestDTO.builder().targetAppointmentId(targetAppointmentId).build());

    assertThat(rescheduled.getId()).isEqualTo(targetAppointmentId);
    assertThat(rescheduled.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    assertThat(rescheduled.getUserId()).isEqualTo(client.getId());

    Appointment originalAfter = appointmentRepository.findById(originalAppointmentId).orElseThrow();
    assertThat(originalAfter.getStatus()).isEqualTo(AppointmentStatus.AVAILABLE);
    assertThat(originalAfter.getUserId()).isNull();

    User persistedClient = userRepository.findById(client.getId()).orElseThrow();
    assertThat(persistedClient.getRemainingAppointments()).isEqualTo(2);
  }

  @Test
  void givenBookedTargetSlot_whenClientReschedules_thenThrowsAppointmentNotAvailableException() {
    UserDTO client = createActiveClient(seed(), 3);
    UserDTO otherClient = createActiveClient(seed(), 3);
    Long originalAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    Long targetAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(11, 0), LocalTime.of(12, 0));

    authenticateAs(otherClient.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(targetAppointmentId).build());

    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(originalAppointmentId).build());
    UpdateAppointmentRequestDTO rescheduleRequest =
        UpdateAppointmentRequestDTO.builder().targetAppointmentId(targetAppointmentId).build();

    assertThatThrownBy(() -> service.updateAppointment(originalAppointmentId, rescheduleRequest))
        .isInstanceOf(AppointmentNotAvailableException.class);
  }

  @Test
  void givenBookedAppointmentLessThan12hAway_whenAdminCancels_thenSucceedsWithoutWindowCheck() {
    UserDTO client = createActiveClient(seed(), 3);
    UserDTO admin = createActiveAdmin(seed());
    Long appointmentId = createAppointmentForCancelWindowTest(LocalDateTime.now().plusMinutes(5));

    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    authenticateAs(admin.getId(), "ADMIN");
    AppointmentDTO canceled =
        service.updateAppointment(appointmentId, UpdateAppointmentRequestDTO.builder().build());

    assertThat(canceled.getStatus()).isEqualTo(AppointmentStatus.AVAILABLE);
  }

  @Test
  void givenClientOwnAppointments_whenGetByUserId_thenReturnsOwnAppointments() {
    UserDTO client = createActiveClient(seed(), 3);
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    List<AppointmentDTO> appointments = service.getByUserId(client.getId());

    assertThat(appointments).extracting(AppointmentDTO::getId).contains(appointmentId);
  }

  @Test
  void givenClient_whenGetByUserIdForAnotherUser_thenThrowsAppointmentOwnershipException() {
    UserDTO client = createActiveClient(seed(), 3);
    UserDTO otherClient = createActiveClient(seed(), 3);
    authenticateAs(client.getId(), "CLIENT");
    Long otherClientId = otherClient.getId();

    assertThatThrownBy(() -> service.getByUserId(otherClientId))
        .isInstanceOf(AppointmentOwnershipException.class);
  }

  @Test
  void givenAdmin_whenGetByUserIdForAnyUser_thenSucceeds() {
    UserDTO client = createActiveClient(seed(), 3);
    UserDTO admin = createActiveAdmin(seed());
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(appointmentId).build());

    authenticateAs(admin.getId(), "ADMIN");
    List<AppointmentDTO> appointments = service.getByUserId(client.getId());

    assertThat(appointments).extracting(AppointmentDTO::getId).contains(appointmentId);
  }

  @Test
  void givenAvailableAndBookedAppointments_whenGetAllAppointments_thenReturnsOnlyBooked() {
    UserDTO client = createActiveClient(seed(), 3);
    Long bookedAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    Long availableAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(11, 0), LocalTime.of(12, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(bookedAppointmentId).build());

    List<AppointmentDTO> appointments =
        service.getAllAppointments(AppointmentSearchRequestDTO.builder().build());

    assertThat(appointments).extracting(AppointmentDTO::getId).contains(bookedAppointmentId);
    assertThat(appointments)
        .extracting(AppointmentDTO::getId)
        .doesNotContain(availableAppointmentId);
    assertThat(appointments)
        .allSatisfy(dto -> assertThat(dto.getStatus()).isEqualTo(AppointmentStatus.BOOKED));
  }

  @Test
  void givenUserIdFilter_whenGetAllAppointments_thenReturnsOnlyMatchingUser() {
    UserDTO firstClient = createActiveClient(seed(), 3);
    UserDTO secondClient = createActiveClient(seed(), 3);
    Long firstAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    Long secondAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(11, 0), LocalTime.of(12, 0));
    authenticateAs(firstClient.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(firstAppointmentId).build());
    authenticateAs(secondClient.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(secondAppointmentId).build());

    List<AppointmentDTO> appointments =
        service.getAllAppointments(
            AppointmentSearchRequestDTO.builder().userId(firstClient.getId()).build());

    assertThat(appointments).extracting(AppointmentDTO::getId).containsExactly(firstAppointmentId);
  }

  @Test
  void givenPilatesIdFilter_whenGetAllAppointments_thenReturnsOnlyMatchingPilates() {
    UserDTO client = createActiveClient(seed(), 3);
    Long firstAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    Long secondAppointmentId =
        createAppointment(farFutureDate(), LocalTime.of(11, 0), LocalTime.of(12, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(firstAppointmentId).build());
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(secondAppointmentId).build());
    Long firstPilatesId =
        appointmentRepository.findById(firstAppointmentId).orElseThrow().getPilatesId();

    List<AppointmentDTO> appointments =
        service.getAllAppointments(
            AppointmentSearchRequestDTO.builder().pilatesId(firstPilatesId).build());

    assertThat(appointments).extracting(AppointmentDTO::getId).containsExactly(firstAppointmentId);
  }

  @Test
  void givenDateRangeFilter_whenGetAllAppointments_thenReturnsOnlyWithinRange() {
    UserDTO client = createActiveClient(seed(), 3);
    LocalDate matchingDate = farFutureDate();
    LocalDate otherDate = matchingDate.plusDays(10);
    Long matchingAppointmentId =
        createAppointment(matchingDate, LocalTime.of(9, 0), LocalTime.of(10, 0));
    Long otherAppointmentId = createAppointment(otherDate, LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(client.getId(), "CLIENT");
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(matchingAppointmentId).build());
    service.bookAppointment(
        BookAppointmentRequestDTO.builder().appointmentId(otherAppointmentId).build());

    List<AppointmentDTO> appointments =
        service.getAllAppointments(
            AppointmentSearchRequestDTO.builder()
                .dateFrom(matchingDate)
                .dateTo(matchingDate)
                .build());

    assertThat(appointments)
        .extracting(AppointmentDTO::getId)
        .containsExactly(matchingAppointmentId);
  }

  @Test
  void
      givenAvailableAppointmentOnSpecificDate_whenGetAvailableWithDateFilter_thenReturnsOnlyMatchingDate() {
    LocalDate matchingDate = farFutureDate();
    LocalDate otherDate = matchingDate.plusDays(1);
    Long matchingAppointmentId =
        createAppointment(matchingDate, LocalTime.of(9, 0), LocalTime.of(10, 0));
    createAppointment(otherDate, LocalTime.of(9, 0), LocalTime.of(10, 0));

    UserDTO client = createActiveClient(seed(), 3);
    authenticateAs(client.getId(), "CLIENT");

    List<AppointmentDTO> available = service.getAvailableAppointments(matchingDate);

    assertThat(available).extracting(AppointmentDTO::getId).contains(matchingAppointmentId);
    assertThat(available)
        .allSatisfy(dto -> assertThat(dto.getTerminDate()).isEqualTo(matchingDate));
  }

  @Test
  void givenAdmin_whenDeleteAppointment_thenRemovesRowPermanently() {
    UserDTO admin = createActiveAdmin(seed());
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    authenticateAs(admin.getId(), "ADMIN");

    service.deleteAppointment(appointmentId);

    assertThat(appointmentRepository.findById(appointmentId)).isEmpty();
  }

  private UserDTO createActiveClient(String seed, int remainingAppointments) {
    UserDTO created =
        userService.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.appt.client." + seed)
                .fullName("Appointment Test Client")
                .email("itest.appt.client." + seed + "@fitme.com")
                .phoneNumber("+3816" + seed + "1")
                .password("itest.appt.fitme123!")
                .remainingAppointments(remainingAppointments)
                .build());
    return userService.updateUser(
        created.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());
  }

  private UserDTO createActiveAdmin(String seed) {
    UserDTO created =
        userService.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.appt.admin." + seed)
                .fullName("Appointment Test Admin")
                .email("itest.appt.admin." + seed + "@fitme.com")
                .phoneNumber("+3816" + seed + "2")
                .password("itest.appt.fitme123!")
                .build());
    return userService.updateUser(
        created.getId(),
        UpdateUserRequestDTO.builder().status(Status.ACTIVE).roles(List.of(Role.ADMIN)).build());
  }

  private Long createAppointment(LocalDate date, LocalTime startTime, LocalTime endTime) {
    TerminDTO termin =
        terminService.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .build());
    PilatesDTO pilates =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("P." + seed()).name("Reformer").build());

    return appointmentRepository.findAll().stream()
        .filter(
            a -> a.getTerminId().equals(termin.getId()) && a.getPilatesId().equals(pilates.getId()))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private Long createAppointmentForCancelWindowTest(LocalDateTime terminStart) {
    LocalTime startTime = terminStart.toLocalTime();
    return createAppointment(terminStart.toLocalDate(), startTime, capEndOfDay(startTime, 30));
  }

  private LocalTime capEndOfDay(LocalTime startTime, long durationMinutes) {
    LocalTime endTime = startTime.plusMinutes(durationMinutes);
    return endTime.isAfter(startTime) ? endTime : LocalTime.of(23, 59);
  }

  private void authenticateAs(Long userId, String roleCode) {
    User user = userRepository.findById(userId).orElseThrow();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_" + roleCode)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private LocalDate farFutureDate() {
    return LocalDate.now().plusDays(2 + (System.nanoTime() % 1000));
  }

  private String seed() {
    return String.valueOf(System.nanoTime());
  }
}
