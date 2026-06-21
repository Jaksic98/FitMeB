package com.consi.fitme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuccessResponseDTO<T> {
  private boolean success;
  private String message;
  private T entity;
  private long timestamp;
  private String path;
}
