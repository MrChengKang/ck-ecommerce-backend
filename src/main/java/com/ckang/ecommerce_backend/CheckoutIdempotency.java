package com.ckang.ecommerce_backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "checkout_idempotency", uniqueConstraints = @UniqueConstraint(
    name = "uk_checkout_idempotency_customer_key",
    columnNames = { "customer_id", "idempotency_key" }))
public class CheckoutIdempotency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "idempotency_key", nullable = false, length = 36)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "order_id")
  private Long orderId;

  public Long getId() {
    return id;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public void setRequestHash(String requestHash) {
    this.requestHash = requestHash;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }
}
