package com.consi.fitme.dto.response;

import com.consi.fitme.dto.base.BaseMessageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageResponseDTO extends BaseMessageDTO {
  public MessageResponseDTO(String message) {
    super(message);
  }
}
