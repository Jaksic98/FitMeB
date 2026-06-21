package com.consi.fitme.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponseDTO {
  private final int code;
  private final String message;
  private final List<String> details;
  private final long timestamp;
}
