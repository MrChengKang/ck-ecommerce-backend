package com.ckang.ecommerce_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
// 💡 優化：允許前端所有常用的請求方法
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
    RequestMethod.PUT, RequestMethod.DELETE })
public class ProductController {

  @Autowired
  private ProductRepository productRepository;

  // 🚨 修正：如果你有 ProductService 就注入它，如果沒有就直接用 Repository
  // @Autowired
  // private ProductService productService;

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
    try {
      // 💡 這裡改用 productRepository 直接儲存 (最簡單穩定的做法)
      Product savedProduct = productRepository.save(product);
      return ResponseEntity.ok(savedProduct);
    } catch (Exception e) {
      e.printStackTrace(); // 在後台噴出具體錯誤，方便你 Debug
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

  // --- 5. 更新商品 ---
  @PutMapping("/{id}")
  public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
    return productRepository.findById(id).map(product -> {
      product.setName(productDetails.getName());
      product.setPrice(productDetails.getPrice());
      product.setDescription(productDetails.getDescription());
      product.setImageUrl(productDetails.getImageUrl());
      product.setCategory(productDetails.getCategory());
      return ResponseEntity.ok(productRepository.save(product));
    }).orElse(ResponseEntity.notFound().build());
  }

  // --- 6. 圖片上傳 (選用) ---
  @PostMapping("/upload")
  public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
    String uploadDir = "uploads/";
    Path uploadPath = Paths.get(uploadDir);
    if (!Files.exists(uploadPath))
      Files.createDirectories(uploadPath);

    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    Path filePath = uploadPath.resolve(fileName);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    return ResponseEntity.ok("http://localhost:8080/uploads/" + fileName);
  }
}