package com.ckang.ecommerce_backend;

public record OrderShippedEvent(
    Long orderId,
    String orderNumber,
    String customerEmail,
    String shippingCarrier,
    String trackingNumber,
    String trackingUrl) {
}