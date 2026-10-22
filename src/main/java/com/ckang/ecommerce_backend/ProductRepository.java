package com.ckang.ecommerce_backend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id = :id")
  Optional<Product> findByIdForUpdate(@Param("id") Long id);

  @Query(value = "SELECT DATE(created_at) as date, COUNT(id) as count " +
      "FROM products " +
      "WHERE created_at >= CURRENT_DATE - INTERVAL 6 DAY " +
      "GROUP BY DATE(created_at) " +
      "ORDER BY date ASC", nativeQuery = true)
  List<Object[]> getDailyProductCount();
}