package com.ckang.ecommerce_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path uploadPath = Paths.get("./uploads").toAbsolutePath().normalize();

    File dir = uploadPath.toFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }

    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploadPath.toUri().toString());
  }
}