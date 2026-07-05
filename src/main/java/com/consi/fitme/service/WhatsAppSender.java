package com.consi.fitme.service;

import java.util.List;

public interface WhatsAppSender {
  void sendTemplate(String toPhoneNumber, String templateName, List<String> placeholders);
}
