package com.consi.fitme.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

  @NotBlank(message = "Email je obavezan")
  @Email(message = "Email nije u ispravnom formatu")
  @Size(max = 254, message = "Email moze imati najviše 254 karaktera")
  private String email;

  @NotBlank(message = "Lozinka je obavezna")
  @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 karaktera")
  private String password;
}
