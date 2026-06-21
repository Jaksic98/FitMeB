package com.consi.fitme.dto.request;

import com.consi.fitme.dto.base.BaseUserDTO;
import com.consi.fitme.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
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
public class CreateUserRequestDTO extends BaseUserDTO {

  @NotBlank(message = "Lozinka je obavezna")
  @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 karaktera")
  @Pattern(
      regexp = "^(?=.*\\d)(?=.*[\\W_]).{8,72}$",
      message = "Lozinka mora sadrzati bar jedan broj i jedan specijalni karakter")
  private String password;

  @Builder.Default private List<Role> roles = new ArrayList<>();
}
