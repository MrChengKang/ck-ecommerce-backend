package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

  @Autowired
  private CheckoutIdempotencyRepository checkoutIdempotencyRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  // @GetMapping
  // public List<Order> getAllOrders() {
  // return orderRepository.findAll();
  // }

  private List<OrderItemResponse> toItemResponses(Order order) {
    if (order.getItems() == null) {
      return List.of();
    }

    return order.getItems().stream()
        .map(item -> new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getPrice(),
            item.getQuantity(),
            item.getImageUrl()))
        .toList();
  }

  private CustomerOrderResponse toCustomerOrderResponse(Order order) {
    return new CustomerOrderResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getOrderDate(),
        order.getStatus(),
        order.getTotalAmount(),
        toItemResponses(order));
  }

  private AdminOrderResponse toAdminOrderResponse(Order order) {
    return new AdminOrderResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getCustomerName(),
        order.getCustomerEmail(),
        order.getShippingAddress(),
        order.getOrderDate(),
        order.getStatus(),
        order.getTotalAmount(),
        toItemResponses(order),
        order.isArchived(),
        order.getArchivedAt());
  }

  private static final Set<String> ALLOWED_STATUSES = Set.of("Pending", "Shipped", "Completed", "Cancelled");
  private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
      "Pending", Set.of("Shipped", "Cancelled"),
      "Shipped", Set.of("Completed"),
      "Completed", Set.of(),
      "Cancelled", Set.of());

  @Transactional
  @PatchMapping("/{id}/status")
  public ResponseEntity<?> updateStatus(
      @PathVariable Long id,
      @RequestBody(required = false) Map<String, String> payload) {

    String newStatus = payload == null ? null : payload.get("status");

    if (newStatus == null || !ALLOWED_STATUSES.contains(newStatus)) {
      return ResponseEntity.badRequest().body("Invalid order status");
    }

    Order order = orderRepository.findByIdForUpdate(id).orElse(null);
    if (order == null) {
      return ResponseEntity.notFound().build();
    }
    if (order.isArchived()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("Archived orders must be restored before updating their status");
    }

    String currentStatus = normalizeCurrentStatus(order.getStatus());

    if (currentStatus == null) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Current order status is not recognized");
    }

    if (!currentStatus.equals(newStatus)
        && !ALLOWED_STATUS_TRANSITIONS.get(currentStatus).contains(newStatus)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("Order status cannot change from " + currentStatus + " to " + newStatus);
    }

    if (currentStatus.equals(newStatus)) {
      if (!newStatus.equals(order.getStatus())) {
        order.setStatus(newStatus);
        orderRepository.save(order);
      }
      return ResponseEntity.ok("Status already set");
    }

    if ("Pending".equals(currentStatus) && "Cancelled".equals(newStatus)
        && !restoreInventoryForOrder(order)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("Could not safely restore inventory for this order");
    }

    order.setStatus(newStatus);
    orderRepository.save(order);
    return ResponseEntity.ok("Status updated");
  }

  private boolean restoreInventoryForOrder(Order order) {
    Map<Long, Long> quantityByProduct = new HashMap<>();

    if (order.getItems() == null || order.getItems().isEmpty()) {
      return true;
    }

    for (OrderItem item : order.getItems()) {
      if (item == null || item.getProductId() == null ||
          item.getQuantity() == null || item.getQuantity() <= 0) {
        return false;
      }

      long combined = quantityByProduct.getOrDefault(item.getProductId(), 0L)
          + item.getQuantity();
      if (combined > Integer.MAX_VALUE) {
        return false;
      }

      quantityByProduct.put(item.getProductId(), combined);
    }

    Map<Long, Product> lockedProducts = new LinkedHashMap<>();
    List<Long> productIds = quantityByProduct.keySet().stream().sorted().toList();

    for (Long productId : productIds) {
      Product product = productRepository.findByIdForUpdate(productId).orElse(null);
      if (product == null || product.getStockQuantity() == null ||
          (long) product.getStockQuantity() + quantityByProduct.get(productId) > Integer.MAX_VALUE) {
        return false;
      }

      lockedProducts.put(productId, product);
    }

    for (Long productId : productIds) {
      Product product = lockedProducts.get(productId);
      product.setStockQuantity((int) (product.getStockQuantity() + quantityByProduct.get(productId)));
    }

    return true;
  }

  private String normalizeCurrentStatus(String status) {
    if (status == null) {
      return null;
    }

    if ("Delivered".equalsIgnoreCase(status)) {
      return "Completed";
    }

    return ALLOWED_STATUSES.stream()
        .filter(allowedStatus -> allowedStatus.equalsIgnoreCase(status))
        .findFirst()
        .orElse(null);
  }

  @Transactional
  @PostMapping("/checkout")
  public ResponseEntity<?> checkout(
      @RequestBody CheckoutRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String rawIdempotencyKey,
      Authentication authentication) {
    if (authentication == null ||
        request == null ||
        request.items() == null ||
        request.items().isEmpty()) {
      return ResponseEntity.badRequest()
          .body("Cart is empty or user is not authenticated");
    }

    User user = userRepository.findByUsername(authentication.getName())
        .orElse(null);

    if (user == null) {
      return ResponseEntity.status(401).body("User not found");
    }

    String idempotencyKey = rawIdempotencyKey == null ? "" : rawIdempotencyKey.trim();
    if (!idempotencyKey.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
      return ResponseEntity.badRequest().body("A valid Idempotency-Key UUID is required");
    }
    idempotencyKey = idempotencyKey.toLowerCase(java.util.Locale.ROOT);

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

    String requestHash;
    try {
      requestHash = hashCheckoutRequest(request, quantityByProduct);
    } catch (JsonProcessingException e) {
      return ResponseEntity.internalServerError().body("Could not validate checkout request");
    }

    // The MySQL upsert serializes concurrent requests using the same customer/key pair.
    checkoutIdempotencyRepository.reserveKey(user.getId(), idempotencyKey, requestHash);
    CheckoutIdempotency checkoutRecord = checkoutIdempotencyRepository
        .findForUpdate(user.getId(), idempotencyKey)
        .orElseThrow(() -> new IllegalStateException("Checkout idempotency record was not created"));

    if (!checkoutRecord.getRequestHash().equals(requestHash)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("Idempotency-Key was already used for a different checkout request");
    }

    if (checkoutRecord.getOrderId() != null) {
      // Use a locking/current read so a retry can see the order committed by the
      // request that held this idempotency key immediately before it.
      Order existingOrder = orderRepository.findByIdForUpdate(checkoutRecord.getOrderId()).orElse(null);
      if (existingOrder == null) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("The order for this checkout key no longer exists");
      }
      return ResponseEntity.ok(existingOrder);
    }

    Map<Long, Product> products = new HashMap<>();

    List<Long> productIds = quantityByProduct.keySet().stream()
        .sorted()
        .toList();

    for (Long productId : productIds) {
      Product product = productRepository.findByIdForUpdate(productId).orElse(null);

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
    // DATETIME has no timezone, so store a UTC timestamp by convention.
    order.setOrderDate(LocalDateTime.now(ZoneOffset.UTC));
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

    Order savedOrder = orderRepository.saveAndFlush(order);
    checkoutRecord.setOrderId(savedOrder.getId());
    checkoutIdempotencyRepository.save(checkoutRecord);
    return ResponseEntity.ok(savedOrder);
  }

  private String hashCheckoutRequest(
      CheckoutRequest request,
      Map<Long, Integer> quantityByProduct) throws JsonProcessingException {
    Map<String, Object> canonicalRequest = new LinkedHashMap<>();
    canonicalRequest.put("customerName", request.customerName());
    canonicalRequest.put("shippingAddress", request.shippingAddress());
    canonicalRequest.put("paymentMethod", request.paymentMethod());
    canonicalRequest.put("items", quantityByProduct.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> List.of(entry.getKey(), entry.getValue()))
        .toList());

    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(objectMapper.writeValueAsBytes(canonicalRequest));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  @Transactional
  @DeleteMapping("/{id}")
  public ResponseEntity<?> archiveOrder(@PathVariable Long id) {
    try {
      Order order = orderRepository.findByIdForUpdate(id).orElse(null);
      if (order == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
      }

      if (order.isArchived()) {
        return ResponseEntity.ok("Order is already archived");
      }

      String currentStatus = normalizeCurrentStatus(order.getStatus());
      if (currentStatus == null) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Current order status is not recognized");
      }

      if ("Pending".equals(currentStatus)) {
        if (!restoreInventoryForOrder(order)) {
          return ResponseEntity.status(HttpStatus.CONFLICT)
              .body("Could not safely restore inventory for this order");
        }
        order.setStatus("Cancelled");
      }

      order.setArchived(true);
      order.setArchivedAt(LocalDateTime.now());
      orderRepository.save(order);
      return ResponseEntity.ok("Order #" + id + " archived successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Archive failed: " + e.getMessage());
    }
  }

  @Transactional
  @PostMapping("/{id}/restore")
  public ResponseEntity<?> restoreArchivedOrder(@PathVariable Long id) {
    Order order = orderRepository.findByIdForUpdate(id).orElse(null);
    if (order == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
    }
    if (!order.isArchived()) {
      return ResponseEntity.ok("Order is already active");
    }

    order.setArchived(false);
    order.setArchivedAt(null);
    orderRepository.save(order);
    return ResponseEntity.ok("Order #" + id + " restored successfully");
  }

  @GetMapping
  public ResponseEntity<Page<AdminOrderResponse>> getOrders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {

    Pageable pageable = PageRequest.of(
        page, size, Sort.by("orderDate").descending());

    Page<AdminOrderResponse> result = orderRepository.findByArchivedFalse(pageable)
        .map(this::toAdminOrderResponse);

    return ResponseEntity.ok(result);
  }

  @GetMapping("/archived")
  public ResponseEntity<Page<AdminOrderResponse>> getArchivedOrders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    Page<AdminOrderResponse> result = orderRepository.findByArchivedTrue(pageable)
        .map(this::toAdminOrderResponse);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/my-orders")
  public ResponseEntity<List<CustomerOrderResponse>> getMyOrders(
      Authentication authentication) {

    if (authentication == null) {
      return ResponseEntity.status(401).build();
    }

    User user = userRepository.findByUsername(authentication.getName())
        .orElse(null);

    if (user == null) {
      return ResponseEntity.status(401).build();
    }

    List<CustomerOrderResponse> result = orderRepository.findByCustomerId(user.getId()).stream()
        .map(this::toCustomerOrderResponse)
        .toList();

    return ResponseEntity.ok(result);
  }

}
