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
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    String username = null;
    String jwtToken = null;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      jwtToken = authHeader.substring(7);
      try {
        username = jwtUtils.getUsernameFromToken(jwtToken);
      } catch (Exception e) {
        logger.error("JWT Token 解析失敗: " + e.getMessage());
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      if (jwtUtils.validateToken(jwtToken)) {

        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null && user.getRole() != null) {
          var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              user.getUsername(), null, List.of(authority));

          authentication.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}