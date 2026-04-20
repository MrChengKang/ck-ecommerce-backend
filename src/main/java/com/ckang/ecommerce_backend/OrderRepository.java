package com.ckang.ecommerce_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  Long countByCustomerEmail(String customerEmail);

  @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customerEmail = :email")
  Double sumTotalAmountByCustomerEmail(@Param("email") String email);

  @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status != 'CANCELLED'")
  Double getTotalRevenue();

  long count();

  List<Order> findByCustomerId(Long customerId);

  @Query(value = "SELECT DATE(order_date) as date, SUM(total_amount) as amount " +
      "FROM orders " +
      "WHERE order_date >= CURRENT_DATE - INTERVAL 6 DAY " +
      "GROUP BY DATE(order_date) " +
      "ORDER BY date ASC", nativeQuery = true)
  List<Object[]> getDailyRevenue();

  @Query(value = "SELECT DATE(order_date) as date, COUNT(id) as count " +
      "FROM orders " +
      "WHERE order_date >= CURRENT_DATE - INTERVAL 6 DAY " +
      "GROUP BY DATE(order_date) " +
      "ORDER BY date ASC", nativeQuery = true)
  List<Object[]> getDailyOrderCount();

  Page<Order> findAll(Pageable pageable);

}