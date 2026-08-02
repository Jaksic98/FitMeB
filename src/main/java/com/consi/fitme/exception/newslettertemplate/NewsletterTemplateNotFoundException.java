package com.consi.fitme.exception.newslettertemplate;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class NewsletterTemplateNotFoundException extends NotFoundException {
  public NewsletterTemplateNotFoundException(Long id) {
    super(
        "Šablon newsletter-a nije pronađen za ID: " + id, ErrorCode.NEWSLETTER_TEMPLATE_NOT_FOUND);
  }
}
