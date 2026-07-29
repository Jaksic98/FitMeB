package com.consi.fitme.service;

import java.util.List;

public interface EmailService {
  void sendTemplate(String toEmail, String templateName, List<String> placeholders);
}
