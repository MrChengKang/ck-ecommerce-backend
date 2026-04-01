package com.ckang.ecommerce_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 1. 取得專案根目錄下的 uploads 資料夾絕對路徑
    // 這樣不管你在哪裡啟動專案，路徑都不會跑掉
    Path uploadDir = Paths.get("uploads");
    String uploadPath = uploadDir.toFile().getAbsolutePath();

    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadPath + "/");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // 2. 順便把 CORS 也寫在這裡，確保前端 5173 讀取圖片或 API 不會被擋
    registry.addMapping("/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}