package com.consi.fitme.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {

  @NotBlank(message = "Trenutna lozinka je obavezna")
  private String currentPassword;

  @NotBlank(message = "Lozinka je obavezna")
  @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 karaktera")
  @Pattern(
      regexp = "^(?=.*\\d)(?=.*[\\W_]).{8,72}$",
      message = "Lozinka mora sadržati bar jedan broj i jedan specijalni karakter")
  private String newPassword;

  @NotBlank(message = "Potvrda lozinke je obavezna")
  private String confirmNewPassword;

  @AssertTrue(message = "Nove lozinke se ne poklapaju")
  private boolean isPasswordsMatching() {
    return newPassword != null && newPassword.equals(confirmNewPassword);
  }
}
