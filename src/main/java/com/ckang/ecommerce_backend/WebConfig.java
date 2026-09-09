package com.ckang.ecommerce_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadDir = System.getProperty("user.dir") + "/uploads/";
    File dir = new File(uploadDir);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:uploads/");
  }
}