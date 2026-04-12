package com.ckang.ecommerce_backend;

import java.util.HashMap;
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

  @GetMapping("/api/admin/stats")
  public ResponseEntity<?> getDashboardStats() {

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalRevenue", orderRepository.getTotalRevenue());
    stats.put("totalOrders", orderRepository.count());
    stats.put("totalProducts", productRepository.count());
    stats.put("totalCustomers", userRepository.countByRole("USER"));
    return ResponseEntity.ok(stats);
  }
}
