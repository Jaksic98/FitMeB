package com.consi.fitme.service;

import com.consi.fitme.dto.NewsletterSendResultDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterService {

  private final UserRepository userRepository;
  private final EmailService emailService;

  public NewsletterSendResultDTO sendNewsletter(String subject, String htmlContent) {
    List<User> recipients = userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true);

    int sentCount = 0;
    int failedCount = 0;
    for (User recipient : recipients) {
      try {
        emailService.sendRaw(recipient.getEmail(), subject, htmlContent);
        sentCount++;
      } catch (Exception ex) {
        failedCount++;
        log.error(
            "Slanje newsletter-a nije uspelo: email={}, error={}",
            recipient.getEmail(),
            ex.getMessage(),
            ex);
      }
    }

    return NewsletterSendResultDTO.builder()
        .totalRecipients(recipients.size())
        .sentCount(sentCount)
        .failedCount(failedCount)
        .build();
  }
}
