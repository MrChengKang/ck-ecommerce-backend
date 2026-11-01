package com.ckang.ecommerce_backend;

public record MyProductReviewResponse(Long id, Long orderId, Long productId) {
  public static MyProductReviewResponse from(ProductReview review) {
    return new MyProductReviewResponse(review.getId(), review.getOrderId(), review.getProductId());
  }
}
