package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class AppointmentNotBookedException extends CustomException {
  public AppointmentNotBookedException() {
    super(
        "Appointment nije rezervisan, otkazivanje/izmena nije moguća",
        ErrorCode.APPOINTMENT_NOT_BOOKED);
  }
}
