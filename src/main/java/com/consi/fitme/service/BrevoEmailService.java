package com.consi.fitme.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrevoEmailService implements EmailService {

  private static final String TEMPLATE_RESOURCE_PATTERN = "email-templates/%s.html";
  private static final String DEFAULT_SUBJECT = "FitMe Pilates";
  private static final Map<String, String> SUBJECTS_BY_TEMPLATE =
      Map.of(
          "fitme_otp", "Vaš verifikacioni kod — FitMe",
          "fitme_reminder", "Podsetnik za termin — FitMe",
          "fitme_password_reset", "Reset lozinke — FitMe",
          "fitme_password_changed", "Vaša lozinka je promenjena — FitMe",
          "email-booking-confirmation", "Rezervacija potvrđena — FitMe",
          "email-booking-cancellation", "Termin otkazan — FitMe",
          "email-booking-reschedule", "Termin premešten — FitMe");

  private final BrevoEmailClient brevoEmailClient;

  @Override
  public void sendTemplate(String toEmail, String templateName, List<String> placeholders) {
    String htmlContent = loadTemplate(templateName);
    for (int i = 0; i < placeholders.size(); i++) {
      htmlContent = htmlContent.replace("{{" + i + "}}", placeholders.get(i));
    }
    String subject = SUBJECTS_BY_TEMPLATE.getOrDefault(templateName, DEFAULT_SUBJECT);
    brevoEmailClient.send(toEmail, subject, htmlContent);
  }

  @Override
  public void sendTemplate(String toEmail, String templateName, Map<String, String> placeholders) {
    String htmlContent = loadTemplate(templateName);
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      htmlContent = htmlContent.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    String subject = SUBJECTS_BY_TEMPLATE.getOrDefault(templateName, DEFAULT_SUBJECT);
    brevoEmailClient.send(toEmail, subject, htmlContent);
  }

  @Override
  public void sendRaw(String toEmail, String subject, String htmlContent) {
    brevoEmailClient.send(toEmail, subject, htmlContent);
  }

  private String loadTemplate(String templateName) {
    Resource resource = new ClassPathResource(TEMPLATE_RESOURCE_PATTERN.formatted(templateName));
    try (InputStream inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new EmailSendException("Email template nije pronađen: " + templateName, ex);
    }
  }
}
