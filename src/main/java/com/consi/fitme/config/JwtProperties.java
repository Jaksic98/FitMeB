package com.consi.fitme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
  private String secretKey;
  private String cookieName;
  private int cookieExpiry;
  private boolean cookieSecure = true;
}
