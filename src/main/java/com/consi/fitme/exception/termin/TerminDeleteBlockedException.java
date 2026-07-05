package com.consi.fitme.exception.termin;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class TerminDeleteBlockedException extends CustomException {
  public TerminDeleteBlockedException(Long id) {
    super(
        "Termin nije moguće obrisati jer ima rezervisane appointmente, ID: " + id,
        ErrorCode.TERMIN_DELETE_BLOCKED);
  }
}
