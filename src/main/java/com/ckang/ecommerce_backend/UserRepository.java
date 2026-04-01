package com.ckang.ecommerce_backend;

import com.ckang.ecommerce_backend.User; // 👈 確保對準你的 User Entity
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
}
