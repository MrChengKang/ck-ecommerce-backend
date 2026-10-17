package com.ckang.ecommerce_backend;

import java.util.List;

public record CheckoutRequest(
    String customerName,
    String shippingAddress,
    String paymentMethod,
    List<CheckoutItemRequest> items) {
}