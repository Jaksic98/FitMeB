package com.consi.fitme.dto.request;

import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class UpdateUserRequestDTO {

  @Size(min = 5, max = 100, message = "Korisničko ime mora imati između 5 i 100 karaktera")
  private String username;

  @Size(min = 2, max = 100, message = "Ime i prezime moraju imati između 2 i 100 karaktera")
  private String fullName;

  @Email(message = "Email nije u ispravnom formatu")
  @Size(max = 254, message = "Email može imati najviše 254 karaktera")
  private String email;

  @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 karaktera")
  @Pattern(
      regexp = "^(?=.*\\d)(?=.*[\\W_]).{8,72}$",
      message = "Lozinka mora sadržati bar jedan broj i jedan specijalni karakter")
  private String password;

  private Status status;

  @Builder.Default private List<Role> roles = null;
}
