package com.consi.fitme.exception.pilates;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class PilatesNotFoundException extends NotFoundException {
  public PilatesNotFoundException(Long id) {
    super("Sprava za pilates nije pronađena za ID: " + id, ErrorCode.PILATES_NOT_FOUND);
  }
}
