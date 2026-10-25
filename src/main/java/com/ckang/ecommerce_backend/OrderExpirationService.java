package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OrderExpirationService {

  private static final Logger logger = LoggerFactory.getLogger(OrderExpirationService.class);

  private final OrderRepository orderRepository;
  private final BillplzService billplzService;
  private final InventoryService inventoryService;

  public OrderExpirationService(
      OrderRepository orderRepository,
      BillplzService billplzService,
      InventoryService inventoryService) {
    this.orderRepository = orderRepository;
    this.billplzService = billplzService;
    this.inventoryService = inventoryService;
  }

  @Scheduled(fixedDelayString = "${orders.expiration-scan-interval-ms:60000}")
  @Transactional
  public void expireOverdueOrders() {
    List<Long> expiredOrderIds = orderRepository.findExpiredPendingOrderIds(LocalDateTime.now(ZoneOffset.UTC));
    expiredOrderIds.forEach(this::expireOrder);
  }

  @Transactional
  public void expireOrder(Long orderId) {
    Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
    if (order == null || order.isArchived() || !"Pending".equalsIgnoreCase(order.getStatus()) ||
        "PAID".equalsIgnoreCase(order.getPaymentStatus()) || order.getPaymentExpiresAt() == null ||
        order.getPaymentExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
      return;
    }

    // Do not release stock if Billplz could not confirm the unpaid bill was removed.
    if (!billplzService.deleteUnpaidBill(order.getBillplzBillId())) {
      logger.warn("Could not expire order {} because Billplz did not confirm its bill is unpaid", orderId);
      return;
    }
    if (!inventoryService.restoreInventory(order)) {
      logger.error("Could not expire order {} because inventory could not be restored", orderId);
      return;
    }

    order.setStatus("Cancelled");
    orderRepository.save(order);
  }
}
