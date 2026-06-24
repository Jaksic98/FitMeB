package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class AppointmentNotAvailableException extends CustomException {
  public AppointmentNotAvailableException() {
    super("Appointment slot nije dostupan", ErrorCode.APPOINTMENT_NOT_AVAILABLE);
  }
}
