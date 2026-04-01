package com.ckang.ecommerce_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/products")
// 這裡建議加上 * 或者確保前端 port 正確，避免 CORS 擋住
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

  @Autowired
  private ProductRepository productRepository;

  // --- 1. 你漏掉的這個：獲取所有商品 (用於首頁) ---
  @GetMapping
  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }

  // --- 2. 獲取單一商品 (用於詳情頁) ---
  @GetMapping("/{id}")
  public Product getProductById(@PathVariable Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
  }

  // 1. 新增商品
  @PostMapping
  public Product createProduct(@RequestBody Product product) {
    return productRepository.save(product);
  }

  // 2. 刪除商品
  @DeleteMapping("/{id}")
  public void deleteProduct(@PathVariable Long id) {
    productRepository.deleteById(id);
  }

  // 3. 更新商品 (選做)
  @PutMapping("/{id}")
  public Product updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
    Product product = productRepository.findById(id).orElseThrow();
    product.setName(productDetails.getName());
    product.setPrice(productDetails.getPrice());
    product.setDescription(productDetails.getDescription());
    product.setImageUrl(productDetails.getImageUrl());
    product.setCategory(productDetails.getCategory());
    return productRepository.save(product);
  }

  @PostMapping("/upload")
  public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
    // 1. 定義儲存路徑 (這會在你專案根目錄建立一個 uploads 資料夾)
    String uploadDir = "uploads/";
    Path uploadPath = Paths.get(uploadDir);

    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
    }

    // 2. 儲存檔案
    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    Path filePath = uploadPath.resolve(fileName);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    // 3. 回傳圖片的訪問網址 (假設後端跑在 8080)
    return "http://localhost:8080/uploads/" + fileName;
  }

}