package com.consi.fitme.service;

import com.consi.fitme.config.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IcsCalendarService {

  private static final ZoneId EVENT_ZONE = ZoneId.of("Europe/Belgrade");
  private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
  private static final DateTimeFormatter UTC_STAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
  private static final String LOCATION = "Kedrova 12, Beograd";
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final JwtProperties jwtProperties;

  /**
   * Signed, unguessable token embedded in the emailed .ics link so it opens directly in the phone's
   * calendar app without requiring the recipient to be logged in in that browser.
   */
  public String generateIcsAccessToken(Long appointmentId) {
    return hmac(appointmentId);
  }

  public boolean isIcsAccessTokenValid(Long appointmentId, String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    byte[] expected = hmac(appointmentId).getBytes(StandardCharsets.UTF_8);
    byte[] actual = token.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, actual);
  }

  private String hmac(Long appointmentId) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretKey());
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
      byte[] result =
          mac.doFinal(("appointment-ics:" + appointmentId).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Ne mogu da generišem ICS token", e);
    }
  }

  public byte[] buildIcsFile(
      Long appointmentId, String title, LocalDate date, LocalTime start, LocalTime end) {
    String uid = "appointment-" + appointmentId + "@fitme.rs";
    String dtStamp = ZonedDateTime.now(UTC_ZONE).format(UTC_STAMP_FORMAT);
    String ics =
        "BEGIN:VCALENDAR\r\n"
            + "VERSION:2.0\r\n"
            + "PRODID:-//FitMe//Pilates Booking//SR\r\n"
            + "CALSCALE:GREGORIAN\r\n"
            + "METHOD:PUBLISH\r\n"
            + "BEGIN:VEVENT\r\n"
            + "UID:"
            + uid
            + "\r\n"
            + "DTSTAMP:"
            + dtStamp
            + "\r\n"
            + "DTSTART:"
            + toUtcStamp(date, start)
            + "\r\n"
            + "DTEND:"
            + toUtcStamp(date, end)
            + "\r\n"
            + "SUMMARY:"
            + escapeText(title)
            + "\r\n"
            + "LOCATION:"
            + escapeText(LOCATION)
            + "\r\n"
            + "DESCRIPTION:"
            + escapeText("FitMe rezervacija — " + title)
            + "\r\n"
            + "END:VEVENT\r\n"
            + "END:VCALENDAR\r\n";
    return ics.getBytes(StandardCharsets.UTF_8);
  }

  private String toUtcStamp(LocalDate date, LocalTime time) {
    return ZonedDateTime.of(date, time, EVENT_ZONE)
        .withZoneSameInstant(UTC_ZONE)
        .format(UTC_STAMP_FORMAT);
  }

  private String escapeText(String value) {
    return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
  }
}
