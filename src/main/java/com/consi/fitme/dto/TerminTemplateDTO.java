package com.consi.fitme.dto;

import com.consi.fitme.model.Status;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TerminTemplateDTO {

  private final Long id;
  private final DayOfWeek dayOfWeek;
  private final LocalTime startTime;
  private final LocalTime endTime;
  private final Status status;
}
