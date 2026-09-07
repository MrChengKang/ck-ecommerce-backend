package com.ckang.ecommerce_backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // 1. 從 Request Header 取得 Authorization
    String authHeader = request.getHeader("Authorization");

    String username = null;
    String jwtToken = null;

    // 2. 檢查是否有 Bearer 前綴，並提取 Token
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      jwtToken = authHeader.substring(7);
      try {
        username = jwtUtils.getUsernameFromToken(jwtToken);
      } catch (Exception e) {
        logger.error("JWT Token 解析失敗: " + e.getMessage());
      }
    }

    // 3. 驗證 Token 並設置 SecurityContext
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // 驗證 Token 是否有效
      if (jwtUtils.validateToken(jwtToken)) {

        // 建立 Authentication 物件 (此處可根據你專案的需求帶入 Role/Authority)
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            username, null, Collections.emptyList());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 標記為已登入狀態
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    // 4. 繼續執行下一個 Filter 或 Controller
    filterChain.doFilter(request, response);
  }
}