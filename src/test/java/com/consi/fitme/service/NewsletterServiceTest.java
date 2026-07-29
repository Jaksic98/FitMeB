package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.consi.fitme.dto.NewsletterSendResultDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private EmailService emailService;

  private NewsletterService newsletterService;

  @BeforeEach
  void setUp() {
    newsletterService = new NewsletterService(userRepository, emailService);
  }

  @Test
  void sendNewsletter_sendsToActiveUsersWithEmailNotificationsEnabled() {
    User user1 = User.builder().email("user1@example.com").build();
    User user2 = User.builder().email("user2@example.com").build();
    when(userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true))
        .thenReturn(List.of(user1, user2));

    NewsletterSendResultDTO result = newsletterService.sendNewsletter("Subject", "<p>Body</p>");

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

    NewsletterSendResultDTO result = newsletterService.sendNewsletter("Subject", "<p>Body</p>");

    assertThat(result.getTotalRecipients()).isEqualTo(2);
    assertThat(result.getSentCount()).isEqualTo(1);
    assertThat(result.getFailedCount()).isEqualTo(1);
    verify(emailService).sendRaw(eq("user2@example.com"), eq("Subject"), eq("<p>Body</p>"));
  }

  @Test
  void sendNewsletter_noRecipients_returnsZeroedResult() {
    when(userRepository.findByStatusAndEmailNotifications(Status.ACTIVE, true))
        .thenReturn(List.of());

    NewsletterSendResultDTO result = newsletterService.sendNewsletter("Subject", "<p>Body</p>");

    assertThat(result.getTotalRecipients()).isZero();
    assertThat(result.getSentCount()).isZero();
    assertThat(result.getFailedCount()).isZero();
  }
}
