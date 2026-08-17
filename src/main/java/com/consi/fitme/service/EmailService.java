package com.consi.fitme.service;

import java.util.List;
import java.util.Map;

public interface EmailService {
  void sendTemplate(String toEmail, String templateName, List<String> placeholders);

  void sendTemplate(String toEmail, String templateName, Map<String, String> placeholders);

  void sendRaw(String toEmail, String subject, String htmlContent);
}
