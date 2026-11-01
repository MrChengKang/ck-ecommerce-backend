package com.ckang.ecommerce_backend;

public record OrderDeliveredEvent(
    Long orderId,
    String orderNumber,
    String customerEmail,
    String trackingNumber,
    String trackingUrl) {

  public static OrderDeliveredEvent fromOrder(Order order) {
    return new OrderDeliveredEvent(
        order.getId(),
        order.getOrderNumber(),
        order.getCustomerEmail(),
        order.getTrackingNumber(),
        order.getTrackingUrl());
  }
}
