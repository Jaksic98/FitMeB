package com.consi.fitme.dto.base;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseUserDTO {

  @NotBlank(message = "Korisničko ime je obavezno")
  @Size(min = 5, max = 100, message = "Korisničko ime mora imati između 5 i 100 karaktera")
  private String username;

  @NotBlank(message = "Ime i prezime su obavezni")
  @Size(min = 2, max = 100, message = "Ime i prezime moraju imati između 2 i 100 karaktera")
  private String fullName;

  @NotBlank(message = "Email je obavezan")
  @Email(message = "Email nije u ispravnom formatu")
  @Size(max = 254, message = "Email može imati najviše 254 karaktera")
  private String email;

  @NotBlank(message = "Broj telefona je obavezan")
  @Pattern(regexp = "^\\+?[0-9 ]{6,30}$", message = "Broj telefona nije u ispravnom formatu")
  private String phoneNumber;
}
