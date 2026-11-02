package com.ckang.ecommerce_backend;

public interface ProductRatingSummary {
  Long getProductId();

  Double getAverageRating();

  Long getReviewCount();
}
