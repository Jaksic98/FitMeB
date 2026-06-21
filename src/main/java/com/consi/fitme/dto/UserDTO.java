package com.consi.fitme.dto;

import com.consi.fitme.dto.base.BaseUserDTO;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class UserDTO extends BaseUserDTO {

  private final Long id;
  private final Status status;
  private final List<Role> roles;
}
