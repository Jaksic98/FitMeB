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
public class VerifyOtpRequestDTO {

  @NotBlank(message = "Broj telefona je obavezan")
  @Pattern(regexp = "^\\+?[0-9 ]{6,30}$", message = "Broj telefona nije u ispravnom formatu")
  private String phoneNumber;

  @NotBlank(message = "Kod je obavezan")
  @Pattern(regexp = "^\\d{6}$", message = "Kod mora biti 6-cifreni broj")
  private String code;
}
