package com.consi.fitme.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequestDTO {

  @NotBlank(message = "Broj telefona je obavezan")
  @Pattern(regexp = "^\\+?[0-9 ]{6,30}$", message = "Broj telefona nije u ispravnom formatu")
  private String phoneNumber;
}
