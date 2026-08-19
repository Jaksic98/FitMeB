package com.consi.fitme.service;

import com.consi.fitme.model.ReminderType;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.model.entity.AppointmentReminder;
import com.consi.fitme.model.entity.Pilates;
import com.consi.fitme.model.entity.Termin;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.AppointmentReminderRepository;
import com.consi.fitme.repository.AppointmentRepository;
import com.consi.fitme.repository.PilatesRepository;
import com.consi.fitme.repository.TerminRepository;
import com.consi.fitme.repository.UserRepository;
import com.consi.fitme.util.AppClock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

  private static final int DAY_BEFORE_HOURS = 24;
  private static final int HOUR_BEFORE_HOURS = 1;

  private final AppointmentReminderRepository repository;
  private final AppointmentRepository appointmentRepository;
  private final TerminRepository terminRepository;
  private final PilatesRepository pilatesRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;

  @Value("${email.templates.reminder:fitme_reminder}")
  private String reminderTemplateName;

  @Value("${frontend.base-url:https://pilates.fitme.rs}")
  private String frontendBaseUrl;

  @Transactional
  public void scheduleReminders(Long appointmentId, LocalDateTime terminStart) {
    repository.save(
        AppointmentReminder.builder()
            .appointmentId(appointmentId)
            .type(ReminderType.DAY_BEFORE)
            .scheduledAt(terminStart.minusHours(DAY_BEFORE_HOURS))
            .build());
    repository.save(
        AppointmentReminder.builder()
            .appointmentId(appointmentId)
            .type(ReminderType.HOUR_BEFORE)
            .scheduledAt(terminStart.minusHours(HOUR_BEFORE_HOURS))
            .build());
  }

  @Transactional
  public void cancelReminders(Long appointmentId) {
    repository.deleteAll(repository.findAllByAppointmentIdAndSentAtIsNull(appointmentId));
  }

  @Transactional
  public void sendDueReminders() {
    repository
        .findAllByScheduledAtLessThanEqualAndSentAtIsNull(AppClock.now())
        .forEach(this::sendReminder);
  }

  private void sendReminder(AppointmentReminder reminder) {
    Appointment appointment =
        appointmentRepository.findById(reminder.getAppointmentId()).orElseThrow();
    User user = userRepository.findById(appointment.getUserId()).orElseThrow();
    Termin termin = terminRepository.findById(appointment.getTerminId()).orElseThrow();
    Pilates pilates = pilatesRepository.findById(appointment.getPilatesId()).orElseThrow();

    try {
      emailService.sendTemplate(
          user.getEmail(),
          reminderTemplateName,
          List.of(
              pilates.getName(),
              termin.getDate().toString(),
              termin.getStartTime().toString(),
              frontendBaseUrl + "/termini"));
    } catch (Exception ex) {
      log.error(
          "Greška pri slanju podsetnika: appointmentId={}, error={}",
          appointment.getId(),
          ex.getMessage(),
          ex);
    }

    reminder.setSentAt(AppClock.now());
    repository.save(reminder);
  }
}
