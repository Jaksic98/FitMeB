package com.consi.fitme.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

public class InfobipWhatsAppSender implements WhatsAppSender {

  private static final Logger logger = LoggerFactory.getLogger(InfobipWhatsAppSender.class);

  private final RestClient restClient;
  private final String apiKey;
  private final String baseUrl;
  private final String sender;

  public InfobipWhatsAppSender(String apiKey, String baseUrl, String sender) {
    this.restClient = RestClient.create();
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.sender = sender;
  }

  @Override
  public void sendTemplate(String toPhoneNumber, String templateName, List<String> placeholders) {
    try {
      Map<String, Object> body =
          Map.of(
              "messages",
              List.of(
                  Map.of(
                      "from",
                      sender,
                      "to",
                      toPhoneNumber,
                      "content",
                      Map.of(
                          "templateName",
                          templateName,
                          "templateData",
                          Map.of("body", Map.of("placeholders", placeholders)),
                          "language",
                          "sr"))));

      restClient
          .post()
          .uri(baseUrl + "/whatsapp/1/message/template")
          .header("Authorization", "App " + apiKey)
          .body(body)
          .retrieve()
          .toBodilessEntity();

      logger.info("WhatsApp poruka poslana: to={}, template={}", toPhoneNumber, templateName);
    } catch (Exception ex) {
      logger.error(
          "Greška pri slanju WhatsApp poruke: to={}, template={}, error={}",
          toPhoneNumber,
          templateName,
          ex.getMessage(),
          ex);
    }
  }
}
