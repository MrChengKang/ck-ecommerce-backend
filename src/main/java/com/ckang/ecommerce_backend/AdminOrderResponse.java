package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
    Long id,
    String orderNumber,
    String customerName,
    String customerEmail,
    String shippingAddress,
    LocalDateTime orderDate,
    String status,
    Double totalAmount,
    List<OrderItemResponse> items) {
}