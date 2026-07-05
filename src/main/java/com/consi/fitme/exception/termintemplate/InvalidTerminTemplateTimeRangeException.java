package com.consi.fitme.exception.termintemplate;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class InvalidTerminTemplateTimeRangeException extends CustomException {
  public InvalidTerminTemplateTimeRangeException() {
    super(
        "Vreme završetka šablona mora biti posle vremena početka",
        ErrorCode.TERMIN_TEMPLATE_INVALID_TIME_RANGE);
  }
}
