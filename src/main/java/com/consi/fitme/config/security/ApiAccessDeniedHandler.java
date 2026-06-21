package com.consi.fitme.config.security;

import com.consi.fitme.model.ErrorCode;
import com.consi.fitme.util.ErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AccessDeniedException accessDeniedException)
      throws IOException {
    ErrorResponseUtil.writeJsonResponse(
        response,
        ErrorCode.ACCESS_DENIED,
        ErrorCode.ACCESS_DENIED.getMessage(),
        List.of("Nemate dozvolu za pristup ovom resursu"));
  }
}
