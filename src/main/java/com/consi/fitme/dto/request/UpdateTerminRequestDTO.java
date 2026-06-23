package com.consi.fitme.dto.request;

import com.consi.fitme.model.Status;
import java.time.LocalDate;
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
public class UpdateTerminRequestDTO {

  private LocalDate date;

  private LocalTime startTime;

  private LocalTime endTime;

  private Status status;
}
