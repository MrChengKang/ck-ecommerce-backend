package com.ckang.ecommerce_backend;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "wishlist")
@Data
public class Wishlist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  public Wishlist() {
  }

  public Wishlist(Long userId, Product product) {
    this.userId = userId;
    this.product = product;
  }
}