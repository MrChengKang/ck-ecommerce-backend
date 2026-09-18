package com.ckang.ecommerce_backend;

import com.ckang.ecommerce_backend.Product;
import com.ckang.ecommerce_backend.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "*")
public class WishlistController {

  @Autowired
  private WishlistService wishlistService;

  // 💡 F5 重新整理時發送此請求，從 DB 取得完整清單
  @GetMapping
  public ResponseEntity<List<Product>> getWishlist(@RequestHeader("Authorization") String token) {
    List<Product> wishlist = wishlistService.getUserWishlist(token);
    return ResponseEntity.ok(wishlist);
  }

  // 💡 新增/移除收藏項目
  @PostMapping("/toggle/{productId}")
  public ResponseEntity<?> toggleWishlist(
      @RequestHeader("Authorization") String token,
      @PathVariable Long productId) {
    wishlistService.toggleWishlist(token, productId);
    return ResponseEntity.ok().build();
  }

  // 💡 清空願望清單 (Clear All)
  @DeleteMapping
  public ResponseEntity<?> clearWishlist(@RequestHeader("Authorization") String token) {
    wishlistService.clearUserWishlist(token);
    return ResponseEntity.ok().build();
  }
}