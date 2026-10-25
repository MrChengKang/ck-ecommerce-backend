package com.ckang.ecommerce_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  @Autowired
  private ProductRepository productRepository;

  // --- 1. 獲取所有商品 ---
  @GetMapping
  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }

  // --- 2. 獲取單一商品 ---
  @GetMapping("/{id}")
  public ResponseEntity<Product> getProductById(@PathVariable Long id) {
    return productRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // --- 3. 新增商品 ---
  @PostMapping
  public ResponseEntity<?> addProduct(@RequestBody Product product) {
    if (product == null || !hasValidStock(product.getStockQuantity())) {
      return ResponseEntity.badRequest().body("Stock quantity must be a non-negative whole number");
    }

    try {
      Product savedProduct = productRepository.save(product);
      return ResponseEntity.ok(savedProduct);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(400).body("Error saving product: " + e.getMessage());
    }
  }

  // --- 4. 刪除商品 ---
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
    try {
      productRepository.deleteById(id);
      return ResponseEntity.ok("Product deleted successfully");
    } catch (Exception e) {
      return ResponseEntity.status(404).body("Product not found");
    }
  }

  // --- 5. 更新完整商品（修復：補上 stockQuantity） ---
  @Transactional
  @PutMapping("/{id}")
  public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
    if (productDetails == null || !hasValidStock(productDetails.getStockQuantity())) {
      return ResponseEntity.badRequest().body("Stock quantity must be a non-negative whole number");
    }

    return productRepository.findByIdForUpdate(id).map(product -> {
      product.setName(productDetails.getName());
      product.setPrice(productDetails.getPrice());
      product.setDescription(productDetails.getDescription());
      product.setImageUrl(productDetails.getImageUrl());
      product.setCategory(productDetails.getCategory());
      product.setStockQuantity(productDetails.getStockQuantity());
      return ResponseEntity.ok(productRepository.save(product));
    }).orElse(ResponseEntity.notFound().build());
  }

  // --- Quick inventory adjustment ---
  @Transactional
  @PatchMapping("/{id}/stock")
  public ResponseEntity<?> updateStockOnly(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
    Object value = payload == null ? null : payload.get("stockQuantity");
    Integer stockQuantity = null;
    if (value instanceof Integer integer) {
      stockQuantity = integer;
    } else if (value instanceof Long longValue && longValue <= Integer.MAX_VALUE && longValue >= 0) {
      stockQuantity = longValue.intValue();
    }

    if (!hasValidStock(stockQuantity)) {
      return ResponseEntity.badRequest().body("Stock quantity must be a non-negative whole number");
    }

    Product product = productRepository.findByIdForUpdate(id).orElse(null);
    if (product == null) {
      return ResponseEntity.notFound().build();
    }

    product.setStockQuantity(stockQuantity);
    return ResponseEntity.ok(productRepository.save(product));
  }

  private boolean hasValidStock(Integer stockQuantity) {
    return stockQuantity != null && stockQuantity >= 0;
  }

  // --- 6. 圖片上傳 ---
  @PostMapping("/upload")
  public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
    try {
      String uploadDir = System.getProperty("user.dir") + "/uploads/";
      Path uploadPath = Paths.get(uploadDir);

      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
      Path filePath = uploadPath.resolve(fileName);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // 💡 回傳相對路徑
      String imageUrl = "/uploads/" + fileName;

      return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
    }
  }
}
