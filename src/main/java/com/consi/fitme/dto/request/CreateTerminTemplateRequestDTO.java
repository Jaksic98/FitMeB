package com.consi.fitme.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
public class CreateTerminTemplateRequestDTO {

  @NotNull(message = "Dan u nedelji je obavezan")
  private DayOfWeek dayOfWeek;

  @NotNull(message = "Vreme početka je obavezno")
  private LocalTime startTime;

  @NotNull(message = "Vreme završetka je obavezno")
  private LocalTime endTime;
}
