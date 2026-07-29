package com.consi.fitme.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingEmailService implements EmailService {

  @Override
  public void sendTemplate(String toEmail, String templateName, List<String> placeholders) {
    log.info(
        "E-mail (log-only stub, Brevo integracija u pripremi): to={}, template={}, placeholders={}",
        toEmail,
        templateName,
        placeholders);
  }
}
