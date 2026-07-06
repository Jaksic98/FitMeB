package com.consi.fitme.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

public class MetaCloudApiWhatsAppSender implements WhatsAppSender {

  private static final Logger logger = LoggerFactory.getLogger(MetaCloudApiWhatsAppSender.class);

  private final RestClient restClient;
  private final String accessToken;
  private final String phoneNumberId;
  private final String graphApiBaseUrl;
  private final String graphApiVersion;

  public MetaCloudApiWhatsAppSender(
      String accessToken, String phoneNumberId, String graphApiBaseUrl, String graphApiVersion) {
    this(accessToken, phoneNumberId, graphApiBaseUrl, graphApiVersion, RestClient.builder());
  }

  public MetaCloudApiWhatsAppSender(
      String accessToken,
      String phoneNumberId,
      String graphApiBaseUrl,
      String graphApiVersion,
      RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
    this.accessToken = accessToken;
    this.phoneNumberId = phoneNumberId;
    this.graphApiBaseUrl = graphApiBaseUrl;
    this.graphApiVersion = graphApiVersion;
  }

  @Override
  public void sendTemplate(String toPhoneNumber, String templateName, List<String> placeholders) {
    try {
      List<Map<String, Object>> parameters =
          placeholders.stream()
              .map(placeholder -> Map.<String, Object>of("type", "text", "text", placeholder))
              .toList();

      Map<String, Object> body =
          Map.of(
              "messaging_product",
              "whatsapp",
              "to",
              toPhoneNumber,
              "type",
              "template",
              "template",
              Map.of(
                  "name",
                  templateName,
                  "language",
                  Map.of("code", "sr"),
                  "components",
                  List.of(Map.of("type", "body", "parameters", parameters))));

      restClient
          .post()
          .uri(graphApiBaseUrl + "/" + graphApiVersion + "/" + phoneNumberId + "/messages")
          .header("Authorization", "Bearer " + accessToken)
          .body(body)
          .retrieve()
          .toBodilessEntity();

      logger.info(
          "WhatsApp poruka poslana (Meta Cloud API): to={}, template={}",
          toPhoneNumber,
          templateName);
    } catch (Exception ex) {
      logger.error(
          "Greška pri slanju WhatsApp poruke (Meta Cloud API): to={}, template={}, error={}",
          toPhoneNumber,
          templateName,
          ex.getMessage(),
          ex);
    }
  }
}
