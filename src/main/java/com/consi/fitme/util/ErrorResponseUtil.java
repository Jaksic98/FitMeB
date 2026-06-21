package com.consi.fitme.util;

import com.consi.fitme.dto.response.ErrorResponseDTO;
import com.consi.fitme.model.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponseUtil {

  public static ErrorResponseDTO body(ErrorCode errorCode, String message, List<String> details) {
    return ErrorResponseDTO.builder()
        .code(errorCode.getCode())
        .message(message)
        .details(details)
        .timestamp(System.currentTimeMillis())
        .build();
  }

  public static ResponseEntity<ErrorResponseDTO> response(
      ErrorCode errorCode, String message, List<String> details) {
    return ResponseEntity.status(errorCode.getHttpStatus()).body(body(errorCode, message, details));
  }

  public static void writeJsonResponse(
      HttpServletResponse response, ErrorCode errorCode, String message, List<String> details)
      throws IOException {
    ErrorResponseDTO payload = body(errorCode, message, details);
    response.setStatus(errorCode.getHttpStatus().value());
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(toJson(payload));
  }

  private static String toJson(ErrorResponseDTO payload) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"code\":").append(payload.getCode()).append(",");
    json.append("\"message\":\"").append(escape(payload.getMessage())).append("\",");
    json.append("\"details\":[");
    if (payload.getDetails() != null) {
      for (int i = 0; i < payload.getDetails().size(); i++) {
        if (i > 0) {
          json.append(",");
        }
        json.append("\"").append(escape(payload.getDetails().get(i))).append("\"");
      }
    }
    json.append("],");
    json.append("\"timestamp\":").append(payload.getTimestamp());
    json.append("}");
    return json.toString();
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
