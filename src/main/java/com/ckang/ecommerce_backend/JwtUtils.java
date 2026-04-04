package com.ckang.ecommerce_backend;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

  // 建議將 Secret 抽出來
  private final String jwtSecret = "ckStoreSecretKey_MustBeAtLeast32CharactersLong!!_2024";

  // 🚨 這裡直接改為 1000 * 10 (10秒)，方便測試
  private final int jwtExpirationMs = 1000 * 60 * 60 * 24;

  public String generateToken(String username) {
    Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        // 🚨 這裡必須使用 jwtExpirationMs，或是你剛才定義的 expirationTime
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // 💡 建議加上這個方法，讓後端真的去校驗過期
  public boolean validateToken(String token) {
    try {
      Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      System.out.println("--- TOKEN EXPIRED ---");
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}