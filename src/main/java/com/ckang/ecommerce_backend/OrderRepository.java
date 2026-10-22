package com.ckang.ecommerce_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        List<Order> findByCustomerEmail(String customerEmail);

        Long countByCustomerEmail(String customerEmail);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT o FROM Order o WHERE o.id = :id")
        Optional<Order> findByIdForUpdate(@Param("id") Long id);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customerEmail = :email")
        Double sumTotalAmountByCustomerEmail(@Param("email") String email);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status != 'CANCELLED'")
        Double getTotalRevenue();

        long count();

        List<Order> findByCustomerId(Long customerId);

        Page<Order> findByArchivedFalse(Pageable pageable);

        Page<Order> findByArchivedTrue(Pageable pageable);

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

}
