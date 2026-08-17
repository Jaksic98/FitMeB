package com.consi.fitme.service;

import com.consi.fitme.model.entity.Pilates;
import com.consi.fitme.model.entity.Termin;
import com.consi.fitme.model.entity.User;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingNotificationService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private final EmailService emailService;
  private final IcsCalendarService icsCalendarService;

  @Value("${email.templates.booking-confirmation:email-booking-confirmation}")
  private String confirmationTemplateName;

  @Value("${email.templates.booking-cancellation:email-booking-cancellation}")
  private String cancellationTemplateName;

  @Value("${email.templates.booking-reschedule:email-booking-reschedule}")
  private String rescheduleTemplateName;

  @Value("${frontend.base-url:https://pilates.fitme.rs}")
  private String frontendBaseUrl;

  public void sendConfirmation(Long appointmentId, User user, Termin termin, Pilates pilates) {
    try {
      Map<String, String> placeholders = new LinkedHashMap<>();
      placeholders.put("FIRST_NAME", firstName(user));
      putTerminPlaceholders(placeholders, termin, pilates);
      placeholders.put("ICS_DOWNLOAD_URL", icsDownloadUrl(appointmentId));
      placeholders.put("MY_APPOINTMENTS_URL", frontendBaseUrl + "/termini");
      placeholders.put("CONTACT_URL", frontendBaseUrl + "/kontakt");

      emailService.sendTemplate(user.getEmail(), confirmationTemplateName, placeholders);
    } catch (Exception ex) {
      log.error(
          "Greška pri slanju potvrde rezervacije: appointmentId={}, error={}",
          appointmentId,
          ex.getMessage(),
          ex);
    }
  }

  public void sendCancellation(
      Long appointmentId, User user, Termin termin, Pilates pilates, boolean creditRefunded) {
    try {
      Map<String, String> placeholders = new LinkedHashMap<>();
      placeholders.put("FIRST_NAME", firstName(user));
      putTerminPlaceholders(placeholders, termin, pilates);
      placeholders.put(
          "CREDIT_REFUND_MESSAGE",
          creditRefunded ? "Kredit je vraćen na Vaš nalog." : "Kredit nije vraćen na Vaš nalog.");
      placeholders.put("BOOKING_URL", frontendBaseUrl + "/termini");
      placeholders.put("CONTACT_URL", frontendBaseUrl + "/kontakt");

      emailService.sendTemplate(user.getEmail(), cancellationTemplateName, placeholders);
    } catch (Exception ex) {
      log.error(
          "Greška pri slanju obaveštenja o otkazivanju: appointmentId={}, error={}",
          appointmentId,
          ex.getMessage(),
          ex);
    }
  }

  public void sendReschedule(
      Long newAppointmentId,
      User user,
      Termin oldTermin,
      Pilates oldPilates,
      Termin newTermin,
      Pilates newPilates) {
    try {
      Map<String, String> placeholders = new LinkedHashMap<>();
      placeholders.put("FIRST_NAME", firstName(user));
      placeholders.put("OLD_DAY_NAME", dayName(oldTermin.getDate().getDayOfWeek()));
      placeholders.put("OLD_DATE", oldTermin.getDate().format(DATE_FORMAT));
      placeholders.put("OLD_TIME", oldTermin.getStartTime().format(TIME_FORMAT));
      putTerminPlaceholders(placeholders, newTermin, newPilates);
      placeholders.put("ICS_DOWNLOAD_URL", icsDownloadUrl(newAppointmentId));
      placeholders.put("MY_APPOINTMENTS_URL", frontendBaseUrl + "/termini");
      placeholders.put("CONTACT_URL", frontendBaseUrl + "/kontakt");

      emailService.sendTemplate(user.getEmail(), rescheduleTemplateName, placeholders);
    } catch (Exception ex) {
      log.error(
          "Greška pri slanju obaveštenja o pomeranju termina: appointmentId={}, error={}",
          newAppointmentId,
          ex.getMessage(),
          ex);
    }
  }

  private void putTerminPlaceholders(
      Map<String, String> placeholders, Termin termin, Pilates pilates) {
    placeholders.put("DAY_NAME", dayName(termin.getDate().getDayOfWeek()));
    placeholders.put("DATE", termin.getDate().format(DATE_FORMAT));
    placeholders.put("TIME", termin.getStartTime().format(TIME_FORMAT));
    placeholders.put("END_TIME", termin.getEndTime().format(TIME_FORMAT));
    placeholders.put("MACHINE_NAME", pilates.getName());
  }

  private String icsDownloadUrl(Long appointmentId) {
    String icsToken = icsCalendarService.generateIcsAccessToken(appointmentId);
    return frontendBaseUrl + "/api/appointments/" + appointmentId + "/ics?token=" + icsToken;
  }

  private String firstName(User user) {
    String fullName = user.getFullName();
    if (fullName == null || fullName.isBlank()) {
      return user.getUsername();
    }
    return fullName.trim().split("\\s+")[0];
  }

  private String dayName(DayOfWeek dayOfWeek) {
    return switch (dayOfWeek) {
      case MONDAY -> "Ponedeljak";
      case TUESDAY -> "Utorak";
      case WEDNESDAY -> "Sreda";
      case THURSDAY -> "Četvrtak";
      case FRIDAY -> "Petak";
      case SATURDAY -> "Subota";
      case SUNDAY -> "Nedelja";
    };
  }
}
