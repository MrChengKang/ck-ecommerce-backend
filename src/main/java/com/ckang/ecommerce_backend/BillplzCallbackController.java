package com.ckang.ecommerce_backend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/billplz")
public class BillplzCallbackController {

  private static final Logger logger = LoggerFactory.getLogger(BillplzCallbackController.class);

  private final BillplzService billplzService;
  private final OrderRepository orderRepository;

  public BillplzCallbackController(
      BillplzService billplzService,
      OrderRepository orderRepository) {
    this.billplzService = billplzService;
    this.orderRepository = orderRepository;
  }

  @Transactional
  @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<?> callback(@RequestParam Map<String, String> params) {
    if (!billplzService.isValidCallbackSignature(params)) {
      logger.warn("Rejected Billplz callback because its signature was invalid");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Invalid Billplz signature");
    }

    String billId = params.get("id");
    if (billId == null || billId.isBlank()) {
      return ResponseEntity.badRequest().body("Missing Billplz bill ID");
    }

    Order order = orderRepository.findByBillplzBillIdForUpdate(billId).orElse(null);
    if (order == null) {
      logger.warn("Received Billplz callback for an unknown bill ID");
      return ResponseEntity.notFound().build();
    }

    boolean paid = Boolean.parseBoolean(params.get("paid"));
    String state = params.get("state");

    // Billplz may send a valid callback for an unpaid/due bill.
    if (!paid || !"paid".equalsIgnoreCase(state)) {
      return ResponseEntity.ok("Callback received");
    }

    String amountText = params.get("amount");
    try {
      long receivedAmount = Long.parseLong(amountText);
      long expectedAmount = BigDecimal.valueOf(order.getTotalAmount())
          .movePointRight(2)
          .setScale(0, RoundingMode.HALF_UP)
          .longValueExact();

      if (receivedAmount != expectedAmount) {
        logger.error("Billplz callback amount mismatch for order {}", order.getId());
        return ResponseEntity.badRequest().body("Payment amount mismatch");
      }
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body("Invalid payment amount");
    }

    // Idempotent: repeated callbacks leave an already-paid order paid.
    order.setPaymentStatus("PAID");
    if ("Pending".equalsIgnoreCase(order.getStatus())) {
      order.setStatus("Processing");
    }
    orderRepository.save(order);
    logger.info("Confirmed Billplz payment for order {}", order.getId());

    return ResponseEntity.ok("Payment confirmed");
  }
}
