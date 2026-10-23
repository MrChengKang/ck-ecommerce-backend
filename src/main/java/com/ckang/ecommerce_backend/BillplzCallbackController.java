package com.ckang.ecommerce_backend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/billplz")
public class BillplzCallbackController {

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
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Invalid Billplz signature");
    }

    String billId = params.get("id");
    if (billId == null || billId.isBlank()) {
      return ResponseEntity.badRequest().body("Missing Billplz bill ID");
    }

    Order order = orderRepository.findByBillplzBillId(billId).orElse(null);
    if (order == null) {
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
        return ResponseEntity.badRequest().body("Payment amount mismatch");
      }
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body("Invalid payment amount");
    }

    // Idempotent: repeated callbacks leave an already-paid order paid.
    order.setPaymentStatus("PAID");
    orderRepository.save(order);

    return ResponseEntity.ok("Payment confirmed");
  }
}