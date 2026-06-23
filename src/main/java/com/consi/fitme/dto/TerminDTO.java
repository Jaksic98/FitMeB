package com.consi.fitme.dto;

import com.consi.fitme.model.Status;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TerminDTO {

  private final Long id;
  private final LocalDate date;
  private final LocalTime startTime;
  private final LocalTime endTime;
  private final Status status;
}
