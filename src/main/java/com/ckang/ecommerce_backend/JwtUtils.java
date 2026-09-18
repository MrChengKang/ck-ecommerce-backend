package com.ckang.ecommerce_backend;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

  private final String jwtSecret = "ckStoreSecretKey_MustBeAtLeast32CharactersLong!!_2024";

  private final int jwtExpirationMs = 1000 * 60 * 60 * 24;

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(String username) {
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String getUsernameFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      System.out.println("--- TOKEN EXPIRED ---");
      return false;
    } catch (Exception e) {
      System.out.println("--- INVALID TOKEN ---");
      return false;
    }
  }
}