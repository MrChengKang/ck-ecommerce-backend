package com.ckang.ecommerce_backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. 徹底關閉 CSRF (這就是導致 403 的元兇)
        .csrf(csrf -> csrf.disable())

        // 2. 啟用 CORS 配置
        .cors(Customizer.withDefaults())

        // 3. 設定路徑權限
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll() // 放行登入接口
            .requestMatchers("/api/products/**").permitAll() // 放行產品接口
            .requestMatchers("/uploads/**").permitAll() // 放行圖片
            .anyRequest().permitAll() // 測試階段：全部放行
        );

    return http.build();
  }

  // 4. 為了確保 Security 能抓到 CORS 設定，建議在這邊也補一個 Bean
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // 前端網址
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}