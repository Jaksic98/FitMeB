package com.consi.fitme.dto;

import com.consi.fitme.model.Status;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PilatesDTO {

  private final Long id;
  private final String position;
  private final String name;
  private final Status status;
}
