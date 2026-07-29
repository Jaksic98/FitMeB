package com.consi.fitme.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrevoEmailClient {

  private static final String BASE_URL = "https://api.brevo.com/v3";

  private final RestClient restClient;
  private final String apiKey;
  private final String senderEmail;
  private final String senderName;

  @Autowired
  public BrevoEmailClient(
      @Value("${brevo.api-key:}") String apiKey,
      @Value("${brevo.sender.email:noreply@pilates.fitme.rs}") String senderEmail,
      @Value("${brevo.sender.name:FitMe Pilates}") String senderName) {
    this(apiKey, senderEmail, senderName, RestClient.builder());
  }

  public BrevoEmailClient(
      String apiKey, String senderEmail, String senderName, RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    this.apiKey = apiKey;
    this.senderEmail = senderEmail;
    this.senderName = senderName;
  }

  public void send(String toEmail, String subject, String htmlContent) {
    Map<String, Object> payload =
        Map.of(
            "sender",
            Map.of("name", senderName, "email", senderEmail),
            "to",
            List.of(Map.of("email", toEmail)),
            "subject",
            subject,
            "htmlContent",
            htmlContent);

    try {
      restClient
          .post()
          .uri("/smtp/email")
          .header("api-key", apiKey)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new EmailSendException(
          "Slanje email-a preko Brevo API-ja nije uspelo: to=" + toEmail, ex);
    }
  }
}
