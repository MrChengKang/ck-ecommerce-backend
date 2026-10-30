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
        LocalDateTime paymentExpiresAt,
        String status,
        String paymentStatus,
        Double totalAmount,
        List<OrderItemResponse> items,
        boolean archived,
        LocalDateTime archivedAt,
        String shippingCarrier,
        String trackingNumber,
        String trackingUrl,
        LocalDateTime shippedAt) {
}
