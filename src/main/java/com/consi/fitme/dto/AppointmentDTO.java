package com.consi.fitme.dto;

import com.consi.fitme.model.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppointmentDTO {

  private final Long id;
  private final Long terminId;
  private final Long pilatesId;
  private final Long userId;
  private final String userFullName;
  private final AppointmentStatus status;
  private final LocalDate terminDate;
  private final LocalTime terminStartTime;
  private final LocalTime terminEndTime;
  private final String pilatesPosition;
  private final String pilatesName;
}
