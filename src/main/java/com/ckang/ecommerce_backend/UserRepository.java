package com.ckang.ecommerce_backend;

import com.ckang.ecommerce_backend.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  long countByRole(String role);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  List<User> findByRole(String role);

  @Query(value = "SELECT DATE(created_at) as date, COUNT(id) as count " +
      "FROM users " +
      "WHERE role = 'USER' AND created_at >= CURRENT_DATE - INTERVAL 6 DAY " +
      "GROUP BY DATE(created_at) " +
      "ORDER BY date ASC", nativeQuery = true)
  List<Object[]> getDailyCustomerCount();
}
