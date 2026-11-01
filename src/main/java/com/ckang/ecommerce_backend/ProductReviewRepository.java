package com.ckang.ecommerce_backend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
  List<ProductReview> findByProductIdAndVisibleTrueOrderByCreatedAtDesc(Long productId);
  List<ProductReview> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
  List<ProductReview> findAllByOrderByCreatedAtDesc();
  boolean existsByOrderIdAndProductId(Long orderId, Long productId);
}
