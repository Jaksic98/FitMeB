package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class AppointmentCancelWindowExpiredException extends CustomException {
  public AppointmentCancelWindowExpiredException() {
    super(
        "Otkazivanje ili izmena nije moguća manje od 12h pre termina",
        ErrorCode.APPOINTMENT_CANCEL_WINDOW_EXPIRED);
  }
}
