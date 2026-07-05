package com.consi.fitme.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopWhatsAppSender implements WhatsAppSender {

  private static final Logger logger = LoggerFactory.getLogger(NoopWhatsAppSender.class);

  @Override
  public void sendTemplate(String toPhoneNumber, String templateName, List<String> placeholders) {
    logger.info(
        "WhatsApp poruka (noop): to={}, template={}, placeholders={}",
        toPhoneNumber,
        templateName,
        placeholders);
  }
}
