package com.consi.fitme.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestInterceptorConfig implements HandlerInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(RequestInterceptorConfig.class);

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    logger.info("Incoming request: [{}] {}", request.getMethod(), request.getRequestURI());
    return true;
  }
}
