package com.consi.fitme.exception.termin;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class TerminOverlapException extends CustomException {
  public TerminOverlapException() {
    super("Termin se preklapa sa postojećim terminom istog datuma", ErrorCode.TERMIN_OVERLAP);
  }
}
