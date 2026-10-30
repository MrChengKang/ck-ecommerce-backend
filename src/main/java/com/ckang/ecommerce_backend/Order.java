package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long customerId;
  private String orderNumber;
  private String customerName;
  private String customerEmail;
  private Double totalAmount;
  private String status;
  @Column(name = "order_date")
  private LocalDateTime orderDate;
  @Column(name = "payment_expires_at")
  private LocalDateTime paymentExpiresAt;
  private String shippingAddress;
  private String paymentMethod;
  @Column(name = "payment_status", length = 20)
  private String paymentStatus = "UNPAID";
  @Column(name = "billplz_bill_id", unique = true, length = 64)
  private String billplzBillId;
  @Column(name = "billplz_payment_url", length = 1024)
  private String billplzPaymentUrl;
  @Column(name = "archived", nullable = false, columnDefinition = "boolean default false")
  private boolean archived = false;
  @Column(name = "archived_at")
  private LocalDateTime archivedAt;

  @Column(name = "shipping_carrier", length = 80)
  private String shippingCarrier;

  @Column(name = "tracking_number", length = 120)
  private String trackingNumber;

  @Column(name = "tracking_url", length = 1024)
  private String trackingUrl;

  @Column(name = "shipped_at")
  private LocalDateTime shippedAt;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "order_id")
  private List<OrderItem> items = new java.util.ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public void setCustomerEmail(String customerEmail) {
    this.customerEmail = customerEmail;
  }

  public Double getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(Double totalAmount) {
    this.totalAmount = totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDateTime orderDate) {
    this.orderDate = orderDate;
  }

  public LocalDateTime getPaymentExpiresAt() {
    return paymentExpiresAt;
  }

  public void setPaymentExpiresAt(LocalDateTime paymentExpiresAt) {
    this.paymentExpiresAt = paymentExpiresAt;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public void setItems(List<OrderItem> items) {
    this.items = items;
  }

  public String getShippingAddress() {
    return shippingAddress;
  }

  public void setShippingAddress(String shippingAddress) {
    this.shippingAddress = shippingAddress;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public LocalDateTime getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(LocalDateTime archivedAt) {
    this.archivedAt = archivedAt;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public String getBillplzBillId() {
    return billplzBillId;
  }

  public void setBillplzBillId(String billplzBillId) {
    this.billplzBillId = billplzBillId;
  }

  public String getBillplzPaymentUrl() {
    return billplzPaymentUrl;
  }

  public void setBillplzPaymentUrl(String billplzPaymentUrl) {
    this.billplzPaymentUrl = billplzPaymentUrl;
  }

  public String getShippingCarrier() {
    return shippingCarrier;
  }

  public void setShippingCarrier(String shippingCarrier) {
    this.shippingCarrier = shippingCarrier;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  public String getTrackingUrl() {
    return trackingUrl;
  }

  public void setTrackingUrl(String trackingUrl) {
    this.trackingUrl = trackingUrl;
  }

  public LocalDateTime getShippedAt() {
    return shippedAt;
  }

  public void setShippedAt(LocalDateTime shippedAt) {
    this.shippedAt = shippedAt;
  }
}
