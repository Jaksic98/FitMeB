package com.consi.fitme.service;

import com.consi.fitme.config.JwtProperties;
import com.consi.fitme.exception.auth.InvalidActivationTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class ActivationTokenService {

  private static final String PURPOSE_CLAIM = "purpose";
  private static final String ACTIVATION_PURPOSE = "ACCOUNT_ACTIVATION";
  private static final long ACTIVATION_TOKEN_VALIDITY_MS = 24L * 60 * 60 * 1000;

  private final JwtProperties jwtProperties;

  public ActivationTokenService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String generateToken(String email) {
    return Jwts.builder()
        .setSubject(email)
        .claim(PURPOSE_CLAIM, ACTIVATION_PURPOSE)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + ACTIVATION_TOKEN_VALIDITY_MS))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String extractEmail(String token) {
    Claims claims;
    try {
      claims =
          Jwts.parserBuilder()
              .setSigningKey(getSignInKey())
              .build()
              .parseClaimsJws(token)
              .getBody();
    } catch (JwtException | IllegalArgumentException ex) {
      throw new InvalidActivationTokenException();
    }

    if (!ACTIVATION_PURPOSE.equals(claims.get(PURPOSE_CLAIM, String.class))) {
      throw new InvalidActivationTokenException();
    }

    return claims.getSubject();
  }

  private Key getSignInKey() {
    byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtProperties.getSecretKey());
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
