package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.ReminderType;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.model.entity.AppointmentReminder;
import com.consi.fitme.repository.AppointmentReminderRepository;
import com.consi.fitme.repository.AppointmentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class AppointmentReminderServiceIT {

  @Autowired private AppointmentReminderService service;
  @Autowired private AppointmentReminderRepository reminderRepository;
  @Autowired private AppointmentRepository appointmentRepository;
  @Autowired private TerminService terminService;
  @Autowired private PilatesService pilatesService;
  @Autowired private UserService userService;

  @MockitoBean private WhatsAppSender whatsAppSender;

  private Long bookedAppointmentId;

  @Test
  void givenAppointment_whenScheduleReminders_thenCreatesDayBeforeAndHourBeforeRows() {
    LocalDate date = farFutureDate();
    Long appointmentId = createAppointment(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
    LocalDateTime terminStart = LocalDateTime.of(date, LocalTime.of(9, 0));

    service.scheduleReminders(appointmentId, terminStart);

    List<AppointmentReminder> reminders =
        reminderRepository.findAllByAppointmentIdAndSentAtIsNull(appointmentId);

    assertThat(reminders).hasSize(2);
    assertThat(reminders)
        .extracting(AppointmentReminder::getType)
        .containsExactlyInAnyOrder(ReminderType.DAY_BEFORE, ReminderType.HOUR_BEFORE);
    assertThat(reminders)
        .filteredOn(r -> r.getType() == ReminderType.DAY_BEFORE)
        .extracting(AppointmentReminder::getScheduledAt)
        .containsExactly(terminStart.minusHours(24));
    assertThat(reminders)
        .filteredOn(r -> r.getType() == ReminderType.HOUR_BEFORE)
        .extracting(AppointmentReminder::getScheduledAt)
        .containsExactly(terminStart.minusHours(1));
    assertThat(reminders).allSatisfy(r -> assertThat(r.getSentAt()).isNull());
  }

  @Test
  void givenUnsentReminders_whenCancelReminders_thenDeletesThem() {
    LocalDate date = farFutureDate();
    Long appointmentId = createAppointment(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
    service.scheduleReminders(appointmentId, LocalDateTime.of(date, LocalTime.of(9, 0)));

    service.cancelReminders(appointmentId);

    assertThat(reminderRepository.findAllByAppointmentIdAndSentAtIsNull(appointmentId)).isEmpty();
  }

  @Test
  void givenSentReminder_whenCancelReminders_thenDoesNotDeleteSentOnes() {
    LocalDate date = farFutureDate();
    Long appointmentId = createAppointment(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
    service.scheduleReminders(appointmentId, LocalDateTime.of(date, LocalTime.of(9, 0)));

    AppointmentReminder sentReminder =
        reminderRepository.findAllByAppointmentIdAndSentAtIsNull(appointmentId).stream()
            .filter(r -> r.getType() == ReminderType.HOUR_BEFORE)
            .findFirst()
            .orElseThrow();
    sentReminder.setSentAt(LocalDateTime.now());
    reminderRepository.save(sentReminder);

    service.cancelReminders(appointmentId);

    assertThat(reminderRepository.findById(sentReminder.getId())).isPresent();
    assertThat(reminderRepository.findAllByAppointmentIdAndSentAtIsNull(appointmentId)).isEmpty();
  }

  @Test
  void givenDueUnsentReminder_whenSendDueReminders_thenSendsWhatsAppTemplateAndMarksSent() {
    String phoneNumber = bookAppointmentForReminderTest();
    AppointmentReminder due =
        reminderRepository.save(
            AppointmentReminder.builder()
                .appointmentId(bookedAppointmentId)
                .type(ReminderType.HOUR_BEFORE)
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build());

    service.sendDueReminders();

    verify(whatsAppSender).sendTemplate(eq(phoneNumber), eq("fitme_reminder"), any());
    assertThat(reminderRepository.findById(due.getId()).orElseThrow().getSentAt()).isNotNull();
  }

  @Test
  void givenNotYetDueReminder_whenSendDueReminders_thenDoesNotSend() {
    bookAppointmentForReminderTest();
    AppointmentReminder notDue =
        reminderRepository.save(
            AppointmentReminder.builder()
                .appointmentId(bookedAppointmentId)
                .type(ReminderType.DAY_BEFORE)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build());

    service.sendDueReminders();

    verify(whatsAppSender, never()).sendTemplate(any(), any(), any());
    assertThat(reminderRepository.findById(notDue.getId()).orElseThrow().getSentAt()).isNull();
  }

  @Test
  void givenAlreadySentReminder_whenSendDueRemindersCalledAgain_thenDoesNotResend() {
    bookAppointmentForReminderTest();
    LocalDateTime alreadySentAt = LocalDateTime.now().minusMinutes(1);
    reminderRepository.save(
        AppointmentReminder.builder()
            .appointmentId(bookedAppointmentId)
            .type(ReminderType.HOUR_BEFORE)
            .scheduledAt(LocalDateTime.now().minusMinutes(5))
            .sentAt(alreadySentAt)
            .build());

    service.sendDueReminders();

    verify(whatsAppSender, never()).sendTemplate(any(), any(), any());
  }

  private String bookAppointmentForReminderTest() {
    String seed = seed();
    Long appointmentId =
        createAppointment(farFutureDate(), LocalTime.of(9, 0), LocalTime.of(10, 0));
    String phoneNumber = "+3817" + seed;
    UserDTO client =
        userService.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.reminder." + seed)
                .fullName("Reminder Test Client")
                .email("itest.reminder." + seed + "@fitme.com")
                .phoneNumber(phoneNumber)
                .password("itest.reminder.fitme123!")
                .build());
    userService.updateUser(
        client.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());

    Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
    appointment.setStatus(AppointmentStatus.BOOKED);
    appointment.setUserId(client.getId());
    appointmentRepository.save(appointment);

    bookedAppointmentId = appointmentId;
    return phoneNumber;
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
            CreatePilatesRequestDTO.builder().position("R." + seed()).name("Reformer").build());

    return appointmentRepository.findAll().stream()
        .filter(
            a -> a.getTerminId().equals(termin.getId()) && a.getPilatesId().equals(pilates.getId()))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private LocalDate farFutureDate() {
    return LocalDate.now().plusDays(2 + (System.nanoTime() % 1000));
  }

  private String seed() {
    return String.valueOf(System.nanoTime());
  }
}
