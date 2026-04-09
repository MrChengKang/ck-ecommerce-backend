package com.ckang.ecommerce_backend;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {
  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  @GetMapping
  public List<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap) {
    Order order = orderRepository.findById(id).orElse(null);
    if (order == null)
      return ResponseEntity.notFound().build();

    String newStatus = statusMap.get("status");
    if (newStatus == null)
      return ResponseEntity.badRequest().body("Status is required");

    order.setStatus(newStatus);
    orderRepository.save(order);
    return ResponseEntity.ok("Status updated");
  }

  @PostMapping("/checkout")
  public ResponseEntity<?> checkout(@RequestBody Order order) {
    try {
      double realTotal = 0.0;

      if (order.getItems() == null || order.getItems().isEmpty()) {
        return ResponseEntity.badRequest().body("購物車是空的");
      }

      for (OrderItem item : order.getItems()) {
        // 💡 確保 productId 不為 null
        if (item.getProductId() == null)
          continue;

        Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new RuntimeException("產品不存在 ID: " + item.getProductId()));

        System.out.println("產品: " + product.getName() + " | 資料庫庫存: " + product.getStockQuantity() + " | 訂單要求數量: "
            + item.getQuantity());

        if (product.getStockQuantity() < item.getQuantity()) {
          return ResponseEntity.badRequest()
              .body("產品 [" + product.getName() + "] 庫存不足！(剩餘: " + product.getStockQuantity() + ")");
        }

        // 💡 3. 執行扣除庫存
        product.setStockQuantity(product.getStockQuantity() - item.getQuantity());

        // 💡 4. 存回產品表 (更新庫存)
        productRepository.save(product);

        // 💡 修正點：確保使用正確的 Setter 名稱
        item.setPrice(product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);

        // 💡 修正點：處理可能的 null 值計算
        double price = (product.getPrice() != null)
            ? product.getPrice().doubleValue()
            : 0.0;
        int qty = (item.getQuantity() != null) ? item.getQuantity() : 0;

        realTotal += price * qty;
      }

      // 💡 確保你的 Order.java 裡變數名是 totalAmount
      order.setTotalAmount(Double.valueOf(realTotal));

      order.setOrderNumber("CK" + System.currentTimeMillis());
      order.setOrderDate(LocalDateTime.now());
      order.setStatus("Pending");

      Order savedOrder = orderRepository.save(order);
      return ResponseEntity.ok(savedOrder);

    } catch (Exception e) {
      e.printStackTrace(); // 在後台印出錯誤細節
      return ResponseEntity.status(500).body("訂單處理失敗: " + e.getMessage());
    }
  }
}