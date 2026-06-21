package com.consi.fitme.util;

import com.consi.fitme.dto.response.SuccessResponseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseUtil {

  public static <T> SuccessResponseDTO<T> success(T data, String message, String path) {
    SuccessResponseDTO<T> response = new SuccessResponseDTO<>();
    response.setSuccess(true);
    response.setMessage(message);
    response.setEntity(data);
    response.setTimestamp(System.currentTimeMillis());
    response.setPath(path);
    return response;
  }
}
