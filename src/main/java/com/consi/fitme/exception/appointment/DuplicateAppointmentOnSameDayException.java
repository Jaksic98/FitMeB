package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class DuplicateAppointmentOnSameDayException extends CustomException {
  public DuplicateAppointmentOnSameDayException() {
    super(
        "Već imate rezervisan termin za izabrani datum", ErrorCode.DUPLICATE_APPOINTMENT_SAME_DAY);
  }
}
