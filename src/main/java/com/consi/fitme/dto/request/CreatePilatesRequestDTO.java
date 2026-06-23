package com.consi.fitme.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreatePilatesRequestDTO {

  @NotBlank(message = "Pozicija je obavezna")
  @Size(max = 50, message = "Pozicija može imati najviše 50 karaktera")
  private String position;

  @NotBlank(message = "Naziv je obavezan")
  @Size(max = 120, message = "Naziv može imati najviše 120 karaktera")
  private String name;
}
