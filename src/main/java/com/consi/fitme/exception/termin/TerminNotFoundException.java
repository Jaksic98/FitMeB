package com.consi.fitme.exception.termin;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class TerminNotFoundException extends NotFoundException {
  public TerminNotFoundException(Long id) {
    super("Termin nije pronađen za ID: " + id, ErrorCode.TERMIN_NOT_FOUND);
  }
}
