package com.ckang.ecommerce_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;

@Service
public class ShipmentNotificationService {
  private static final Logger logger = LoggerFactory.getLogger(ShipmentNotificationService.class);

  private final EmailApiService emailApiService;

  public ShipmentNotificationService(EmailApiService emailApiService) {
    this.emailApiService = emailApiService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendShipmentNotice(OrderShippedEvent event) {
    if (event.customerEmail() == null || event.customerEmail().isBlank()) {
      logger.warn("Cannot send shipment notice for order {}: customer email is missing",
          event.orderId());
      return;
    }

    String orderLabel = event.orderNumber() == null || event.orderNumber().isBlank()
        ? "#" + event.orderId()
        : event.orderNumber();

    StringBuilder body = new StringBuilder()
        .append("Your order ").append(orderLabel).append(" has shipped.\n\n")
        .append("Carrier: ").append(event.shippingCarrier()).append("\n")
        .append("Tracking number: ").append(event.trackingNumber()).append("\n");

    if (event.trackingUrl() != null && !event.trackingUrl().isBlank()) {
      body.append("Track your package: ").append(event.trackingUrl()).append("\n");
    }

    body.append("\nThank you for your purchase.");

    try {
      emailApiService.sendText(event.customerEmail(), "Your order has shipped", body.toString());
      logger.info("Sent shipment notice for order {}", event.orderId());
    } catch (Exception e) {
      logger.error("Could not send shipment notice for order {}", event.orderId(), e);
    }
  }
}
