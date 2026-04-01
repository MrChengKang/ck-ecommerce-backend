package com.ckang.ecommerce_backend;

public class AuthResponse {
  private String token;

  // 建構子 (Constructor)
  public AuthResponse(String token) {
    this.token = token;
  }

  // Getter for token
  public String getToken() {
    return token;
  }

  // Setter for token
  public void setToken(String token) {
    this.token = token;
  }
}
