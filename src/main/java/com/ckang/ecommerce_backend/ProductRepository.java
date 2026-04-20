package com.ckang.ecommerce_backend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
  @Query(value = "SELECT DATE(created_at) as date, COUNT(id) as count " +
      "FROM products " +
      "WHERE created_at >= CURRENT_DATE - INTERVAL 6 DAY " +
      "GROUP BY DATE(created_at) " +
      "ORDER BY date ASC", nativeQuery = true)
  List<Object[]> getDailyProductCount();
}