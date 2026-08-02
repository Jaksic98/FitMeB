package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private NewsletterTemplateRepository newsletterTemplateRepository;
  @Mock private EmailService emailService;

  private NewsletterService newsletterService;

  @BeforeEach
  void setUp() {
    newsletterService =
        new NewsletterService(userRepository, newsletterTemplateRepository, emailService);
  }

  @Test
  void sendNewsletter_sendsToActiveUsersWithEmailNotificationsEnabled() {
    User user1 = User.builder().email("user1@example.com").build();
    User user2 = User.builder().email("user2@example.com").build();
    when(userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true))
        .thenReturn(List.of(user1, user2));

    NewsletterSendResultDTO result =
        newsletterService.sendNewsletter(
            SendNewsletterRequestDTO.builder()
                .subject("Subject")
                .htmlContent("<p>Body</p>")
                .build());

    verify(emailService).sendRaw(eq("user1@example.com"), eq("Subject"), eq("<p>Body</p>"));
    verify(emailService).sendRaw(eq("user2@example.com"), eq("Subject"), eq("<p>Body</p>"));
    assertThat(result.getTotalRecipients()).isEqualTo(2);
    assertThat(result.getSentCount()).isEqualTo(2);
    assertThat(result.getFailedCount()).isZero();
  }

  @Test
  void sendNewsletter_oneRecipientFails_countsFailureAndContinuesBatch() {
    User user1 = User.builder().email("user1@example.com").build();
    User user2 = User.builder().email("user2@example.com").build();
    when(userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true))
        .thenReturn(List.of(user1, user2));
    doThrow(new EmailSendException("boom"))
        .when(emailService)
        .sendRaw(eq("user1@example.com"), eq("Subject"), eq("<p>Body</p>"));

    NewsletterSendResultDTO result =
        newsletterService.sendNewsletter(
            SendNewsletterRequestDTO.builder()
                .subject("Subject")
                .htmlContent("<p>Body</p>")
                .build());

    assertThat(result.getTotalRecipients()).isEqualTo(2);
    assertThat(result.getSentCount()).isEqualTo(1);
    assertThat(result.getFailedCount()).isEqualTo(1);
    verify(emailService).sendRaw(eq("user2@example.com"), eq("Subject"), eq("<p>Body</p>"));
  }

  @Test
  void sendNewsletter_noRecipients_returnsZeroedResult() {
    when(userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true))
        .thenReturn(List.of());

    NewsletterSendResultDTO result =
        newsletterService.sendNewsletter(
            SendNewsletterRequestDTO.builder()
                .subject("Subject")
                .htmlContent("<p>Body</p>")
                .build());

    assertThat(result.getTotalRecipients()).isZero();
    assertThat(result.getSentCount()).isZero();
    assertThat(result.getFailedCount()).isZero();
  }

  @Test
  void sendNewsletter_withTemplateId_usesTemplateHtmlContent() {
    NewsletterTemplate template =
        NewsletterTemplate.builder().id(5L).title("Promo").htmlContent("<p>Iz šablona</p>").build();
    when(newsletterTemplateRepository.findByIdAndStatusNot(5L, Status.DELETED))
        .thenReturn(Optional.of(template));
    User user1 = User.builder().email("user1@example.com").build();
    when(userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true))
        .thenReturn(List.of(user1));

    NewsletterSendResultDTO result =
        newsletterService.sendNewsletter(
            SendNewsletterRequestDTO.builder().subject("Subject").templateId(5L).build());

    verify(emailService).sendRaw(eq("user1@example.com"), eq("Subject"), eq("<p>Iz šablona</p>"));
    assertThat(result.getSentCount()).isEqualTo(1);
  }

  @Test
  void sendNewsletter_unknownTemplateId_throwsNotFound() {
    when(newsletterTemplateRepository.findByIdAndStatusNot(99L, Status.DELETED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                newsletterService.sendNewsletter(
                    SendNewsletterRequestDTO.builder().subject("Subject").templateId(99L).build()))
        .isInstanceOf(NewsletterTemplateNotFoundException.class);
  }

  @Test
  void sendNewsletter_bothTemplateIdAndHtmlContent_throwsInvalidContentSource() {
    assertThatThrownBy(
            () ->
                newsletterService.sendNewsletter(
                    SendNewsletterRequestDTO.builder()
                        .subject("Subject")
                        .templateId(5L)
                        .htmlContent("<p>Body</p>")
                        .build()))
        .isInstanceOf(InvalidNewsletterContentSourceException.class);
  }

  @Test
  void sendNewsletter_neitherTemplateIdNorHtmlContent_throwsInvalidContentSource() {
    assertThatThrownBy(
            () ->
                newsletterService.sendNewsletter(
                    SendNewsletterRequestDTO.builder().subject("Subject").build()))
        .isInstanceOf(InvalidNewsletterContentSourceException.class);
  }
}
