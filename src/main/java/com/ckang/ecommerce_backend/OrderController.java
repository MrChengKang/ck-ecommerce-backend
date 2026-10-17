package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.jaxb.SpringDataJaxb.OrderDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserRepository userRepository;

  // @GetMapping
  // public List<Order> getAllOrders() {
  // return orderRepository.findAll();
  // }

  private static final Set<String> ALLOWED_STATUSES = Set.of("Pending", "Shipped", "Completed", "Cancelled");

  @PatchMapping("/{id}/status")
  public ResponseEntity<?> updateStatus(
      @PathVariable Long id,
      @RequestBody Map<String, String> payload) {
    String newStatus = payload.get("status");

    if (!ALLOWED_STATUSES.contains(newStatus)) {
      return ResponseEntity.badRequest().body("Invalid order status");
    }

    Order order = orderRepository.findById(id).orElse(null);
    if (order == null) {
      return ResponseEntity.notFound().build();
    }

    order.setStatus(newStatus);
    orderRepository.save(order);
    return ResponseEntity.ok("Status updated");
  }

  @Transactional
  @PostMapping("/checkout")
  public ResponseEntity<?> checkout(
      @RequestBody CheckoutRequest request,
      Authentication authentication) {
    if (authentication == null || request.items() == null || request.items().isEmpty()) {
      return ResponseEntity.badRequest().body("Cart is empty or user is not authenticated");
    }

    User user = userRepository.findByUsername(authentication.getName())
        .orElse(null);

    if (user == null) {
      return ResponseEntity.status(401).body("User not found");
    }

    Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();

    for (CheckoutItemRequest item : request.items()) {
      if (item == null || item.productId() == null ||
          item.quantity() == null || item.quantity() <= 0) {
        return ResponseEntity.badRequest().body("Invalid product or quantity");
      }

      long combined = (long) quantityByProduct.getOrDefault(item.productId(), 0)
          + item.quantity();

      if (combined > Integer.MAX_VALUE) {
        return ResponseEntity.badRequest().body("Quantity is too large");
      }

      quantityByProduct.put(item.productId(), (int) combined);
    }

    Map<Long, Product> products = new HashMap<>();

    for (Long productId : quantityByProduct.keySet()) {
      Product product = productRepository.findById(productId).orElse(null);

      if (product == null) {
        return ResponseEntity.badRequest().body("Product not found: " + productId);
      }

      int quantity = quantityByProduct.get(productId);
      if (product.getStockQuantity() == null ||
          product.getStockQuantity() < quantity) {
        return ResponseEntity.badRequest().body(
            "Insufficient stock for product: " + product.getName());
      }

      if (product.getPrice() == null) {
        return ResponseEntity.badRequest().body(
            "Product has no valid price: " + product.getName());
      }

      products.put(productId, product);
    }

    Order order = new Order();
    order.setCustomerId(user.getId());
    order.setCustomerEmail(user.getEmail());
    order.setCustomerName(request.customerName());
    order.setShippingAddress(request.shippingAddress());
    order.setPaymentMethod(request.paymentMethod());
    order.setOrderNumber("CK" + java.util.UUID.randomUUID());
    order.setOrderDate(LocalDateTime.now());
    order.setStatus("Pending");

    BigDecimal total = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();

    for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
      Product product = products.get(entry.getKey());
      int quantity = entry.getValue();

      product.setStockQuantity(product.getStockQuantity() - quantity);

      OrderItem orderItem = new OrderItem();
      orderItem.setProductId(product.getId());
      orderItem.setProductName(product.getName());
      orderItem.setPrice(product.getPrice().doubleValue());
      orderItem.setQuantity(quantity);
      orderItem.setImageUrl(product.getImageUrl());
      orderItems.add(orderItem);

      total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
    }

    order.setItems(orderItems);
    order.setTotalAmount(total.doubleValue());

    return ResponseEntity.ok(orderRepository.save(order));
  }

  @PutMapping("/{id}/status")
  public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
    String newStatus = payload.get("status");
    Order order = orderRepository.findById(id).orElseThrow();
    order.setStatus(newStatus);
    orderRepository.save(order);
    return ResponseEntity.ok("Status updated to " + newStatus);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
    try {
      if (orderRepository.existsById(id)) {
        orderRepository.deleteById(id);
        return ResponseEntity.ok("Order #" + id + " deleted successfully");
      } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
      }
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Delete failed: " + e.getMessage());
    }
  }

  @GetMapping
  public ResponseEntity<Page<Order>> getOrders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    return ResponseEntity.ok(orderRepository.findAll(pageable));
  }

  @GetMapping("/my-orders")
  public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
    if (authentication == null) {
      return ResponseEntity.status(401).build();
    }

    User user = userRepository.findByUsername(authentication.getName())
        .orElse(null);

    if (user == null) {
      return ResponseEntity.status(401).build();
    }

    return ResponseEntity.ok(orderRepository.findByCustomerId(user.getId()));
  }

}