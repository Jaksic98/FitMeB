package com.consi.fitme.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class BookAppointmentRequestDTO {

  @NotNull(message = "ID appointmenta je obavezan")
  private Long appointmentId;

  private Long userId;
}
