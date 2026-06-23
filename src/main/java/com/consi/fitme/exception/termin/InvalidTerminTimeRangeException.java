package com.consi.fitme.exception.termin;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class InvalidTerminTimeRangeException extends CustomException {
  public InvalidTerminTimeRangeException() {
    super(
        "Vreme završetka termina mora biti posle vremena početka",
        ErrorCode.TERMIN_INVALID_TIME_RANGE);
  }
}
