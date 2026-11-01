package com.ckang.ecommerce_backend;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminProductReviewController {
  private final ProductReviewRepository reviewRepository;

  public AdminProductReviewController(ProductReviewRepository reviewRepository) {
    this.reviewRepository = reviewRepository;
  }

  @GetMapping
  public List<AdminProductReviewResponse> getAllReviews() {
    return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(AdminProductReviewResponse::from)
        .toList();
  }

  @PatchMapping("/{id}/visibility")
  @Transactional
  public ResponseEntity<?> setReviewVisibility(
      @PathVariable Long id,
      @RequestBody(required = false) Map<String, Boolean> body) {
    Boolean visible = body == null ? null : body.get("visible");
    if (visible == null) return ResponseEntity.badRequest().body("Visibility is required");
    ProductReview review = reviewRepository.findById(id).orElse(null);
    if (review == null) return ResponseEntity.notFound().build();
    review.setVisible(visible);
    return ResponseEntity.ok(AdminProductReviewResponse.from(reviewRepository.save(review)));
  }
}
