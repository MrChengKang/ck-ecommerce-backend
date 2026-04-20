package com.ckang.ecommerce_backend;

import java.util.List;
import java.util.Map;

public class AdminStatsDTO {
  private long totalRevenue;
  private long totalOrders;
  private long totalProducts;
  private long totalCustomers;
  private List<Map<String, Object>> revenueTrend;
  private List<Map<String, Object>> orderTrend;
  private List<Map<String, Object>> productTrend;
  private List<Map<String, Object>> customerTrend;

  // Constructors
  public AdminStatsDTO() {
  }

  public AdminStatsDTO(long totalRevenue, long totalOrders, long totalProducts, long totalCustomers,
      List<Map<String, Object>> revenueTrend) {
    this.totalRevenue = totalRevenue;
    this.totalOrders = totalOrders;
    this.totalProducts = totalProducts;
    this.totalCustomers = totalCustomers;
    this.revenueTrend = revenueTrend;
  }

  // Getters and Setters
  public long getTotalRevenue() {
    return totalRevenue;
  }

  public void setTotalRevenue(long totalRevenue) {
    this.totalRevenue = totalRevenue;
  }

  // 在 AdminStatsDTO.java 中補上：
  public List<Map<String, Object>> getOrderTrend() {
    return orderTrend;
  }

  public void setOrderTrend(List<Map<String, Object>> orderTrend) {
    this.orderTrend = orderTrend;
  }

  public List<Map<String, Object>> getProductTrend() {
    return productTrend;
  }

  public void setProductTrend(List<Map<String, Object>> productTrend) {
    this.productTrend = productTrend;
  }

  public List<Map<String, Object>> getCustomerTrend() {
    return customerTrend;
  }

  public void setCustomerTrend(List<Map<String, Object>> customerTrend) {
    this.customerTrend = customerTrend;
  }

  public long getTotalOrders() {
    return totalOrders;
  }

  public void setTotalOrders(long totalOrders) {
    this.totalOrders = totalOrders;
  }

  public long getTotalProducts() {
    return totalProducts;
  }

  public void setTotalProducts(long totalProducts) {
    this.totalProducts = totalProducts;
  }

  public long getTotalCustomers() {
    return totalCustomers;
  }

  public void setTotalCustomers(long totalCustomers) {
    this.totalCustomers = totalCustomers;
  }

  public List<Map<String, Object>> getRevenueTrend() {
    return revenueTrend;
  }

  public void setRevenueTrend(List<Map<String, Object>> revenueTrend) {
    this.revenueTrend = revenueTrend;
  }
}
