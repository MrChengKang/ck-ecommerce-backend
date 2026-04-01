package com.ckang.ecommerce_backend;

import jakarta.persistence.*; // 如果你是 Spring Boot 3
// import javax.persistence.*; // 如果你是 Spring Boot 2

@Entity
@Table(name = "users") // 👈 對應你 MySQL 裡的表名
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  private String email;
  private String address;
  private String role;

  // --- 下面是 Getter 和 Setter (一定要有，不然 Repository 抓不到資料) ---

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
