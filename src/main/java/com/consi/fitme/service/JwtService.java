package com.consi.fitme.service;

import com.consi.fitme.config.JwtProperties;
import com.consi.fitme.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtProperties jwtProperties;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
  }

  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    return Jwts.builder()
        .setClaims(extraClaims)
        .setSubject(resolveLoginSubject(userDetails))
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(
            new Date(System.currentTimeMillis() + 1000L * jwtProperties.getCookieExpiry()))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username.equals(resolveLoginSubject(userDetails)) && !isTokenExpired(token);
  }

  public Cookie generateCookie(String token) {
    Cookie cookie = new Cookie(jwtProperties.getCookieName(), token);
    cookie.setHttpOnly(true);
    cookie.setSecure(jwtProperties.isCookieSecure());
    cookie.setPath("/");
    cookie.setMaxAge(jwtProperties.getCookieExpiry());
    return cookie;
  }

  public String extractTokenFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (jwtProperties.getCookieName().equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSignInKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private Key getSignInKey() {
    byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtProperties.getSecretKey());
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String getCookieName() {
    String cookieName = jwtProperties.getCookieName();

    if (cookieName == null || cookieName.isEmpty()) {
      throw new InvalidCookieException("Naziv kolačića nije ispravan");
    }
    return cookieName;
  }

  public boolean isCookieSecure() {
    return jwtProperties.isCookieSecure();
  }

  private String resolveLoginSubject(UserDetails userDetails) {
    if (userDetails instanceof User user && user.getEmail() != null && !user.getEmail().isBlank()) {
      return user.getEmail();
    }
    return userDetails.getUsername();
  }
}
