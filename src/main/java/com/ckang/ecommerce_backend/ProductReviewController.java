package com.ckang.ecommerce_backend;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ProductReviewController {
  private final ProductReviewRepository reviewRepository;
  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;

  public ProductReviewController(
      ProductReviewRepository reviewRepository,
      ProductRepository productRepository,
      OrderRepository orderRepository,
      UserRepository userRepository) {
    this.reviewRepository = reviewRepository;
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
    this.userRepository = userRepository;
  }

  @GetMapping("/products/{productId}")
  public List<ProductReviewResponse> getVisibleReviews(@PathVariable Long productId) {
    return reviewRepository.findByProductIdAndVisibleTrueOrderByCreatedAtDesc(productId).stream()
        .map(ProductReviewResponse::from)
        .toList();
  }

  @GetMapping("/mine")
  public ResponseEntity<?> getMyReviews(Authentication authentication) {
    User customer = getAuthenticatedUser(authentication);
    if (customer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
        .map(MyProductReviewResponse::from)
        .toList());
  }

  @PostMapping
  public ResponseEntity<?> createReview(
      @RequestBody(required = false) ProductReviewRequest request,
      Authentication authentication) {
    User customer = getAuthenticatedUser(authentication);
    if (customer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    if (request == null || request.orderId() == null || request.productId() == null
        || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
      return ResponseEntity.badRequest().body("Order, product, and a 1-to-5 star rating are required");
    }

    String comment = request.comment() == null ? "" : request.comment().trim();
    if (comment.isBlank() || comment.length() > 1000) {
      return ResponseEntity.badRequest().body("Review comment must contain 1 to 1000 characters");
    }

    Order order = orderRepository.findById(request.orderId()).orElse(null);
    if (order == null || !customer.getId().equals(order.getCustomerId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This order does not belong to you");
    }
    if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())
        || !("Completed".equalsIgnoreCase(order.getStatus())
            || "Delivered".equalsIgnoreCase(order.getStatus()))) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("Only paid and delivered orders can be reviewed");
    }
    boolean productWasOrdered = order.getItems().stream()
        .anyMatch(item -> request.productId().equals(item.getProductId()));
    if (!productWasOrdered || !productRepository.existsById(request.productId())) {
      return ResponseEntity.badRequest().body("This product is not part of the selected order");
    }
    if (reviewRepository.existsByOrderIdAndProductId(order.getId(), request.productId())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("You have already reviewed this product for this order");
    }

    ProductReview review = new ProductReview();
    review.setProductId(request.productId());
    review.setOrderId(order.getId());
    review.setCustomerId(customer.getId());
    review.setReviewerName(customer.getUsername());
    review.setRating(request.rating());
    review.setComment(comment);
    review.setVisible(true);

    try {
      ProductReview saved = reviewRepository.saveAndFlush(review);
      return ResponseEntity.status(HttpStatus.CREATED).body(ProductReviewResponse.from(saved));
    } catch (DataIntegrityViolationException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("You have already reviewed this product for this order");
    }
  }

  private User getAuthenticatedUser(Authentication authentication) {
    if (authentication == null) return null;
    return userRepository.findByUsername(authentication.getName()).orElse(null);
  }
}
