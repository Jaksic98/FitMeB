package com.consi.fitme.config;

import com.consi.fitme.service.InfobipWhatsAppSender;
import com.consi.fitme.service.MetaCloudApiWhatsAppSender;
import com.consi.fitme.service.NoopWhatsAppSender;
import com.consi.fitme.service.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WhatsAppSenderConfig {

  private static final String META_GRAPH_API_BASE_URL = "https://graph.facebook.com";

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppSenderConfig.class);

  @Bean
  public WhatsAppSender whatsAppSender(
      @Value("${whatsapp.provider:noop}") String provider,
      @Value("${infobip.api-key:}") String infobipApiKey,
      @Value("${infobip.base-url:}") String infobipBaseUrl,
      @Value("${infobip.whatsapp-sender:}") String infobipSender,
      @Value("${meta.whatsapp.access-token:}") String metaAccessToken,
      @Value("${meta.whatsapp.phone-number-id:}") String metaPhoneNumberId,
      @Value("${meta.whatsapp.graph-api-version:v21.0}") String metaGraphApiVersion) {
    String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();

    return switch (normalizedProvider) {
      case "infobip" -> buildInfobipSender(infobipApiKey, infobipBaseUrl, infobipSender);
      case "meta" -> buildMetaSender(metaAccessToken, metaPhoneNumberId, metaGraphApiVersion);
      case "noop" -> new NoopWhatsAppSender();
      default -> {
        logger.warn(
            "Nepoznat whatsapp.provider '{}' — WhatsApp poruke se samo loguju (noop)", provider);
        yield new NoopWhatsAppSender();
      }
    };
  }

  private WhatsAppSender buildInfobipSender(String apiKey, String baseUrl, String sender) {
    if (apiKey == null || apiKey.isBlank()) {
      logger.warn("whatsapp.provider=infobip ali INFOBIP_API_KEY nije podešen — koristi se noop");
      return new NoopWhatsAppSender();
    }
    return new InfobipWhatsAppSender(apiKey, baseUrl, sender);
  }

  private WhatsAppSender buildMetaSender(
      String accessToken, String phoneNumberId, String graphApiVersion) {
    if (accessToken == null
        || accessToken.isBlank()
        || phoneNumberId == null
        || phoneNumberId.isBlank()) {
      logger.warn(
          "whatsapp.provider=meta ali META_WHATSAPP_ACCESS_TOKEN/META_WHATSAPP_PHONE_NUMBER_ID"
              + " nisu podešeni — koristi se noop");
      return new NoopWhatsAppSender();
    }
    return new MetaCloudApiWhatsAppSender(
        accessToken, phoneNumberId, META_GRAPH_API_BASE_URL, graphApiVersion);
  }
}
