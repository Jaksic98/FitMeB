package com.consi.fitme.config.security;

import com.consi.fitme.model.ErrorCode;
import com.consi.fitme.util.ErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AuthenticationException authException)
      throws IOException {
    ErrorResponseUtil.writeJsonResponse(
        response,
        ErrorCode.AUTHENTICATION_REQUIRED,
        ErrorCode.AUTHENTICATION_REQUIRED.getMessage(),
        List.of("Neophodna je autentikacija za pristup ovom resursu"));
  }
}
