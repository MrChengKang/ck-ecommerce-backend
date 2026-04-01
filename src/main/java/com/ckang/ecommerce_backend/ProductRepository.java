package com.ckang.ecommerce_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
  // 這裡繼承了 JpaRepository，所以自動具備了「找全部」、「存檔」等功能
}