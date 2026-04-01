package com.ckang.ecommerce_backend;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;
  private BigDecimal price;
  private Integer stockQuantity;
  private String imageUrl;
  private String category;
}