package com.ckang.ecommerce_backend;

public class CustomerDTO {
  private Long id;
  private String username;
  private String email;
  private Long totalOrders;
  private Double totalSpent;

  // 💡 空的 Constructor (必須要有)
  public CustomerDTO() {
  }

  // 💡 所有的 Getter 和 Setter (必須要有，不然 Controller 呼叫不到)
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Long getTotalOrders() {
    return totalOrders;
  }

  public void setTotalOrders(Long totalOrders) {
    this.totalOrders = totalOrders;
  }

  public Double getTotalSpent() {
    return totalSpent;
  }

  public void setTotalSpent(Double totalSpent) {
    this.totalSpent = totalSpent;
  }
}
