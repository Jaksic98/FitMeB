package com.consi.fitme.dto.request;

import com.consi.fitme.model.Status;
import jakarta.validation.constraints.Size;
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
public class UpdatePilatesRequestDTO {

  @Size(max = 50, message = "Pozicija može imati najviše 50 karaktera")
  private String position;

  @Size(max = 120, message = "Naziv može imati najviše 120 karaktera")
  private String name;

  private Status status;
}
