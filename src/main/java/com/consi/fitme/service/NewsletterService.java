package com.consi.fitme.service;

import com.consi.fitme.dto.NewsletterSendResultDTO;
import com.consi.fitme.dto.request.SendNewsletterRequestDTO;
import com.consi.fitme.exception.newsletter.InvalidNewsletterContentSourceException;
import com.consi.fitme.exception.newslettertemplate.NewsletterTemplateNotFoundException;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.NewsletterTemplate;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.NewsletterTemplateRepository;
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
  private final NewsletterTemplateRepository newsletterTemplateRepository;
  private final EmailService emailService;

  public NewsletterSendResultDTO sendNewsletter(SendNewsletterRequestDTO sendNewsletterRequestDTO) {
    String subject = sendNewsletterRequestDTO.getSubject();
    String htmlContent = resolveHtmlContent(sendNewsletterRequestDTO);

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

  private String resolveHtmlContent(SendNewsletterRequestDTO sendNewsletterRequestDTO) {
    Long templateId = sendNewsletterRequestDTO.getTemplateId();
    boolean hasHtmlContent =
        sendNewsletterRequestDTO.getHtmlContent() != null
            && !sendNewsletterRequestDTO.getHtmlContent().isBlank();

    if ((templateId != null) == hasHtmlContent) {
      throw new InvalidNewsletterContentSourceException();
    }

    if (templateId != null) {
      NewsletterTemplate template =
          newsletterTemplateRepository
              .findByIdAndStatusNot(templateId, Status.DELETED)
              .orElseThrow(() -> new NewsletterTemplateNotFoundException(templateId));
      return template.getHtmlContent();
    }

    return sendNewsletterRequestDTO.getHtmlContent();
  }
}
