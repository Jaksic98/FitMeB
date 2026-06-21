package com.consi.fitme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final RequestInterceptorConfig requestInterceptorConfig;

  public WebConfig(RequestInterceptorConfig requestInterceptorConfig) {
    this.requestInterceptorConfig = requestInterceptorConfig;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestInterceptorConfig);
  }
}
