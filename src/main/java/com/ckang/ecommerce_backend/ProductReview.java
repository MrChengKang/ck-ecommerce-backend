package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(
    name = "uk_product_review_order_product", columnNames = { "order_id", "product_id" }))
public class ProductReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "reviewer_name", nullable = false, length = 100)
  private String reviewerName;

  @Column(nullable = false)
  private Integer rating;

  @Column(name = "review_text", nullable = false, length = 1000)
  private String comment;

  @Column(name = "is_visible", nullable = false)
  private boolean visible = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public Long getProductId() { return productId; }
  public void setProductId(Long productId) { this.productId = productId; }
  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }
  public Long getCustomerId() { return customerId; }
  public void setCustomerId(Long customerId) { this.customerId = customerId; }
  public String getReviewerName() { return reviewerName; }
  public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
  public Integer getRating() { return rating; }
  public void setRating(Integer rating) { this.rating = rating; }
  public String getComment() { return comment; }
  public void setComment(String comment) { this.comment = comment; }
  public boolean isVisible() { return visible; }
  public void setVisible(boolean visible) { this.visible = visible; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
