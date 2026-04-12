package com.ckang.ecommerce_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // 💡 記得導這個
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  // 💡 1. 根據 Email 統計訂單次數 (對應 Controller 的 countByCustomerEmail)
  Long countByCustomerEmail(String customerEmail);

  // 💡 2. 根據 Email 統計總消費額 (對應 Controller 的 sumTotalAmountByCustomerEmail)
  @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customerEmail = :email")
  Double sumTotalAmountByCustomerEmail(@Param("email") String email);

  // 💡 计算所有订单的总销售额
  @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status != 'CANCELLED'")
  Double getTotalRevenue();

  // 💡 统计总订单数
  long count();

  // 原有的根據 ID 查詢也可以保留
  List<Order> findByCustomerId(Long customerId);
}