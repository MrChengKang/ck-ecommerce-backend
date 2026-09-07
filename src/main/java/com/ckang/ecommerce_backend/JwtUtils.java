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

  // 設定 Token 過期時間為 24 小時 (24 * 60 * 60 * 1000 ms)
  private final int jwtExpirationMs = 1000 * 60 * 60 * 24;

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  // 1. 生成 Token
  public String generateToken(String username) {
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  // 2. 💡 【新增這個方法】從 Token 中解析出 Username (解決紅字報錯)
  public String getUsernameFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  // 3. 驗證 Token 是否有效/未過期
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