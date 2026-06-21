package com.consi.fitme.model;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
  INACTIVE(0),
  ACTIVE(1),
  DELETED(2),
  LOCKED(3);

  private final int code;

  public static Status fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(value -> value.code == code)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown status code: " + code));
  }
}
