package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;

public record AdminProductReviewResponse(
    Long id,
    Long productId,
    Long orderId,
    Long customerId,
    String reviewerName,
    Integer rating,
    String comment,
    boolean visible,
    LocalDateTime createdAt) {
  public static AdminProductReviewResponse from(ProductReview review) {
    return new AdminProductReviewResponse(review.getId(), review.getProductId(), review.getOrderId(),
        review.getCustomerId(), review.getReviewerName(), review.getRating(), review.getComment(),
        review.isVisible(), review.getCreatedAt());
  }
}
