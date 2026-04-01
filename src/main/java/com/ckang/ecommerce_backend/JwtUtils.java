package com.ckang.ecommerce_backend;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

  // 密鑰 (保持這串即可，長度已經大於 32 字元，符合 HS256 標準)
  private String jwtSecret = "ckStoreSecretKey_MustBeAtLeast32CharactersLong!!_2024";
  private int jwtExpirationMs = 86400000; // 24小時

  public String generateToken(String username) {

    // 🚨 修正點 1: 必須將字串轉換為 JWT 專用的 Key 物件
    Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
        // 🚨 修正點 2: 使用 key 物件，並將演算法改為 HS256
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }
}