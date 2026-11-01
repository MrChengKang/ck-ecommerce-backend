package com.ckang.ecommerce_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DeliveryNotificationService {
  private static final Logger logger = LoggerFactory.getLogger(DeliveryNotificationService.class);

  private final EmailApiService emailApiService;

  public DeliveryNotificationService(EmailApiService emailApiService) {
    this.emailApiService = emailApiService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendDeliveryNotice(OrderDeliveredEvent event) {
    if (event.customerEmail() == null || event.customerEmail().isBlank()) {
      logger.warn("Cannot send delivery notice for order {}: customer email is missing",
          event.orderId());
      return;
    }

    String orderLabel = event.orderNumber() == null || event.orderNumber().isBlank()
        ? "#" + event.orderId()
        : event.orderNumber();

    StringBuilder body = new StringBuilder()
        .append("Your order ").append(orderLabel).append(" has been marked as delivered.\n\n");

    if (event.trackingNumber() != null && !event.trackingNumber().isBlank()) {
      body.append("Tracking number: ").append(event.trackingNumber()).append("\n");
    }
    if (event.trackingUrl() != null && !event.trackingUrl().isBlank()) {
      body.append("Track your package: ").append(event.trackingUrl()).append("\n");
    }

    body.append("\nThank you for shopping with us.");

    try {
      emailApiService.sendText(event.customerEmail(), "Your order has been delivered", body.toString());
      logger.info("Sent delivery notice for order {}", event.orderId());
    } catch (Exception e) {
      logger.error("Could not send delivery notice for order {}", event.orderId(), e);
    }
  }
}
