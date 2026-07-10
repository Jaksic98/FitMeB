package com.consi.fitme.dto.request;

import com.consi.fitme.model.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateAppointmentRequestDTO {

  private AppointmentStatus status;
  private Long userId;
  private Long terminId;
  private Long pilatesId;
}
