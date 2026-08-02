package com.consi.fitme.exception.newsletter;

import com.consi.fitme.exception.base.BusinessException;
import com.consi.fitme.model.ErrorCode;

public class InvalidNewsletterContentSourceException extends BusinessException {
  public InvalidNewsletterContentSourceException() {
    super(
        "Potrebno je izabrati šablon ili uneti HTML sadržaj, ali ne oba",
        ErrorCode.NEWSLETTER_INVALID_CONTENT_SOURCE);
  }
}
