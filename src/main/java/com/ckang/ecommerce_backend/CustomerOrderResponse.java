package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderResponse(
        Long id,
        String orderNumber,
        LocalDateTime orderDate,
        LocalDateTime paymentExpiresAt,
        String status,
        String paymentStatus,
        Double totalAmount,
        List<OrderItemResponse> items,
        String shippingCarrier,
        String trackingNumber,
        String trackingUrl,
        LocalDateTime shippedAt) {
}
