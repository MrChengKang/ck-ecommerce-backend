package com.ckang.ecommerce_backend;

import java.util.List;

public record PaymentConfirmedEvent(
    Long orderId,
    String orderNumber,
    String customerEmail,
    Double totalAmount,
    List<Item> items) {

  public static PaymentConfirmedEvent fromOrder(Order order) {
    List<Item> itemSnapshots = order.getItems().stream()
        .map(item -> new Item(item.getProductName(), item.getQuantity(), item.getPrice()))
        .toList();

    return new PaymentConfirmedEvent(
        order.getId(),
        order.getOrderNumber(),
        order.getCustomerEmail(),
        order.getTotalAmount(),
        itemSnapshots);
  }

  public record Item(String productName, Integer quantity, Double unitPrice) {
  }
}
