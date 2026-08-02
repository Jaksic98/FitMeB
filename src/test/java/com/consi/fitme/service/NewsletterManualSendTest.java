package com.consi.fitme.service;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.NewsletterTemplate;
import com.consi.fitme.repository.NewsletterTemplateRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Ručni test — ukloni anotaciju lokalno da pošalješ pravi email preko Brevo-a")
class NewsletterManualSendTest {

  private static final String TO_EMAIL = "your-email@example.com";
  private static final Long TEMPLATE_ID = 1L;

  @Autowired private NewsletterTemplateRepository newsletterTemplateRepository;
  @Autowired private EmailService emailService;

  @Test
  void sendsChosenTemplateToChosenEmail() {
    NewsletterTemplate template =
        newsletterTemplateRepository
            .findByIdAndStatusNot(TEMPLATE_ID, Status.DELETED)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Šablon sa id="
                            + TEMPLATE_ID
                            + " ne postoji u bazi (proveri V16 migraciju)"));

    String subject =
        template.getDefaultSubject() != null && !template.getDefaultSubject().isBlank()
            ? template.getDefaultSubject()
            : "Test newsletter — " + template.getTitle();

    emailService.sendRaw(TO_EMAIL, subject, template.getHtmlContent());

    System.out.println(
        "Poslato na "
            + TO_EMAIL
            + " koristeći šablon '"
            + template.getTitle()
            + "' (id="
            + TEMPLATE_ID
            + ")");
  }
}
