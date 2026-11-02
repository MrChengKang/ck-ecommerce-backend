package com.ckang.ecommerce_backend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
  List<ProductReview> findByProductIdAndVisibleTrueOrderByCreatedAtDesc(Long productId);
  List<ProductReview> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
  List<ProductReview> findAllByOrderByCreatedAtDesc();
  boolean existsByOrderIdAndProductId(Long orderId, Long productId);

  @Query(value = "SELECT product_id AS productId, AVG(rating) AS averageRating, COUNT(*) AS reviewCount " +
      "FROM product_reviews WHERE is_visible = true GROUP BY product_id", nativeQuery = true)
  List<ProductRatingSummary> findVisibleProductRatingSummaries();
}
