package com.ckang.ecommerce_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
  // 繼承 JpaRepository 後，findAll() 和 save() 就自動能用了
}