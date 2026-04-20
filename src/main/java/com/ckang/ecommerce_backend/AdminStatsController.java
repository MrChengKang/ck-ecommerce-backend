package com.ckang.ecommerce_backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminStatsController {
  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserRepository userRepository;

  private List<Map<String, Object>> generateTrend(List<Object[]> dbData) {
    Map<String, Double> dbDataMap = new HashMap<>();
    for (Object[] row : dbData) {
      if (row[0] != null) {
        dbDataMap.put(String.valueOf(row[0]),
            row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0.0);
      }
    }

    List<Map<String, Object>> fullTrend = new ArrayList<>();
    java.time.LocalDate today = java.time.LocalDate.now();

    for (int i = 6; i >= 0; i--) {
      String targetDate = today.minusDays(i).toString();
      Map<String, Object> dayPoint = new HashMap<>();
      dayPoint.put("date", targetDate);
      dayPoint.put("amount", dbDataMap.getOrDefault(targetDate, 0.0));
      fullTrend.add(dayPoint);
    }
    return fullTrend;
  }

  @GetMapping("/api/admin/stats")
  public ResponseEntity<AdminStatsDTO> getDashboardStats() {
    AdminStatsDTO stats = new AdminStatsDTO();

    try {
      // 1. 基本統計數據
      Object revenueObj = orderRepository.getTotalRevenue();
      stats.setTotalRevenue(revenueObj instanceof Number ? ((Number) revenueObj).longValue() : 0L);
      stats.setTotalOrders(orderRepository.count());
      stats.setTotalProducts(productRepository.count());
      stats.setTotalCustomers(userRepository.countByRole("USER"));

      // 2. 獲取數據並直接通過 generateTrend 進行補齊處理
      // 這樣無論是 Revenue 還是 Order，邏輯都統一了
      stats.setRevenueTrend(generateTrend(orderRepository.getDailyRevenue()));
      stats.setOrderTrend(generateTrend(orderRepository.getDailyOrderCount()));
      stats.setProductTrend(generateTrend(productRepository.getDailyProductCount()));
      stats.setCustomerTrend(generateTrend(userRepository.getDailyCustomerCount()));

      return ResponseEntity.ok(stats);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).build();
    }
  }
}
