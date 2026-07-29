package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrevoEmailServiceTest {

  @Mock private BrevoEmailClient brevoEmailClient;

  private BrevoEmailService brevoEmailService;

  @BeforeEach
  void setUp() {
    brevoEmailService = new BrevoEmailService(brevoEmailClient);
  }

  @Test
  void sendTemplate_loadsHtmlFileAndSubstitutesPlaceholders() {
    brevoEmailService.sendTemplate("client@example.com", "fitme_otp", List.of("123456"));

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.captor();
    ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.captor();
    verify(brevoEmailClient)
        .send(eq("client@example.com"), subjectCaptor.capture(), htmlCaptor.capture());

    assertThat(subjectCaptor.getValue()).isEqualTo("Vaš verifikacioni kod — FitMe");
    assertThat(htmlCaptor.getValue()).contains("123456").doesNotContain("{{0}}");
  }

  @Test
  void sendTemplate_reminderTemplate_substitutesAllPlaceholders() {
    brevoEmailService.sendTemplate(
        "client@example.com", "fitme_reminder", List.of("Reformer", "2026-08-01", "18:00"));

    ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.captor();
    verify(brevoEmailClient)
        .send(eq("client@example.com"), eq("Podsetnik za termin — FitMe"), htmlCaptor.capture());

    assertThat(htmlCaptor.getValue())
        .contains("Reformer")
        .contains("2026-08-01")
        .contains("18:00")
        .doesNotContain("{{0}}")
        .doesNotContain("{{1}}")
        .doesNotContain("{{2}}");
  }

  @Test
  void sendTemplate_unknownTemplateName_throwsEmailSendException() {
    assertThatThrownBy(
            () -> brevoEmailService.sendTemplate("client@example.com", "does_not_exist", List.of()))
        .isInstanceOf(EmailSendException.class)
        .hasMessageContaining("does_not_exist");
  }

  @Test
  void sendRaw_delegatesDirectlyToBrevoEmailClient() {
    brevoEmailService.sendRaw("client@example.com", "Newsletter", "<p>Hello</p>");

    verify(brevoEmailClient).send("client@example.com", "Newsletter", "<p>Hello</p>");
  }
}
