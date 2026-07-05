package com.consi.fitme.exception.termintemplate;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class TerminTemplateNotFoundException extends NotFoundException {
  public TerminTemplateNotFoundException(Long id) {
    super("Šablon termina nije pronađen za ID: " + id, ErrorCode.TERMIN_TEMPLATE_NOT_FOUND);
  }
}
