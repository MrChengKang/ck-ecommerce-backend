package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderResponse(
    Long id,
    String orderNumber,
    LocalDateTime orderDate,
    String status,
    Double totalAmount,
    List<OrderItemResponse> items) {
}