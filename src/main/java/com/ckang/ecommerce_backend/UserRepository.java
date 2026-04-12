package com.ckang.ecommerce_backend;

import com.ckang.ecommerce_backend.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  long countByRole(String role);

  Optional<User> findByUsername(String username);

  List<User> findByRole(String role);
}
