package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;

public record ProductReviewResponse(
    Long id,
    Integer rating,
    String comment,
    String reviewerName,
    LocalDateTime createdAt) {
  public static ProductReviewResponse from(ProductReview review) {
    return new ProductReviewResponse(review.getId(), review.getRating(), review.getComment(),
        review.getReviewerName(), review.getCreatedAt());
  }
}
