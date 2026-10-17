package com.ckang.ecommerce_backend;

public record CheckoutItemRequest(
    Long productId,
    Integer quantity) {
}