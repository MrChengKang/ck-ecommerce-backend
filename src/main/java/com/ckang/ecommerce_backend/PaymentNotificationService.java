package com.ckang.ecommerce_backend;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class PaymentNotificationService {
  private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationService.class);

  private final EmailApiService emailApiService;

  public PaymentNotificationService(EmailApiService emailApiService) {
    this.emailApiService = emailApiService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPaymentConfirmation(PaymentConfirmedEvent event) {
    if (event.customerEmail() == null || event.customerEmail().isBlank()) {
      logger.warn("Cannot send payment confirmation for order {}: customer email is missing",
          event.orderId());
      return;
    }

    String orderLabel = event.orderNumber() == null || event.orderNumber().isBlank()
        ? "#" + event.orderId()
        : event.orderNumber();

    StringBuilder body = new StringBuilder()
        .append("Hello,\n\n")
        .append("We have received payment for order ").append(orderLabel).append(".\n\n")
        .append("Items:\n");

    for (PaymentConfirmedEvent.Item item : event.items()) {
      double unitPrice = item.unitPrice() == null ? 0.0 : item.unitPrice();
      int quantity = item.quantity() == null ? 0 : item.quantity();
      body.append("- ").append(item.productName())
          .append(" x ").append(quantity)
          .append(" (RM ").append(String.format(Locale.ROOT, "%.2f", unitPrice)).append(" each)\n");
    }

    double totalAmount = event.totalAmount() == null ? 0.0 : event.totalAmount();
    body.append("\nTotal paid: RM ")
        .append(String.format(Locale.ROOT, "%.2f", totalAmount))
        .append("\n\nThank you for your purchase.");

    try {
      emailApiService.sendText(event.customerEmail(), "Payment confirmed for order " + orderLabel,
          body.toString());
      logger.info("Sent payment confirmation for order {}", event.orderId());
    } catch (Exception e) {
      logger.error("Could not send payment confirmation for order {}", event.orderId(), e);
    }
  }
}
