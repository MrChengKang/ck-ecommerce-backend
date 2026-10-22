package com.ckang.ecommerce_backend;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    Double price,
    Integer quantity,
    String imageUrl) {
}