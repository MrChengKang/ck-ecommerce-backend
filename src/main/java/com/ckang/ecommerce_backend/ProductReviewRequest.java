package com.ckang.ecommerce_backend;

public record ProductReviewRequest(Long orderId, Long productId, Integer rating, String comment) {
}
