package com.consi.fitme.dto.request;

import com.consi.fitme.model.Role;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserSearchRequestDTO extends PagingRequestDTO {

  private Long id;
  private String username;
  private String fullName;
  private String email;
  private List<Role> roles;
  private List<String> status;
}
