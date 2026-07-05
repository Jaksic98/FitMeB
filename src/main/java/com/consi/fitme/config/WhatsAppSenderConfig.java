package com.consi.fitme.config;

import com.consi.fitme.service.InfobipWhatsAppSender;
import com.consi.fitme.service.NoopWhatsAppSender;
import com.consi.fitme.service.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WhatsAppSenderConfig {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppSenderConfig.class);

  @Bean
  public WhatsAppSender whatsAppSender(
      @Value("${infobip.api-key:}") String apiKey,
      @Value("${infobip.base-url:}") String baseUrl,
      @Value("${infobip.whatsapp-sender:}") String sender) {
    if (apiKey == null || apiKey.isBlank()) {
      logger.info("INFOBIP_API_KEY nije podešen — WhatsApp poruke se samo loguju (noop)");
      return new NoopWhatsAppSender();
    }
    return new InfobipWhatsAppSender(apiKey, baseUrl, sender);
  }
}
