package com.ckang.ecommerce_backend;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories") // 👈 確保這行路徑跟前端一致
@CrossOrigin(origins = "http://localhost:5173") // 👈 確保允許前端訪問
public class CategoryController {

  @Autowired
  private CategoryRepository categoryRepository;

  // 獲取所有分類 (對應前端 GET 請求)
  @GetMapping
  public List<Category> getAllCategories() {
    return categoryRepository.findAll();
  }

  // 新增分類 (對應前端 POST 請求)
  @PostMapping
  public Category addCategory(@RequestBody Category category) {
    return categoryRepository.save(category);
  }

  // 刪除分類 (對應前端 DELETE 請求)
  @DeleteMapping("/{id}")
  public void deleteCategory(@PathVariable Long id) {
    categoryRepository.deleteById(id);
  }
}
