package com.ckang.ecommerce_backend;

import com.ckang.ecommerce_backend.Product;
import com.ckang.ecommerce_backend.Wishlist;
import com.ckang.ecommerce_backend.User;
import com.ckang.ecommerce_backend.ProductRepository;
import com.ckang.ecommerce_backend.WishlistRepository;
import com.ckang.ecommerce_backend.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishlistService {

  @Autowired
  private WishlistRepository wishlistRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserRepository userRepository; // 💡 注入 UserRepository 查 User ID

  @Autowired
  private JwtUtils jwtUtils;

  // 💡 透過 Token 解析 Username，再從 DB 取得 UserId
  private Long getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }

    // 1. 從 JwtUtils 取得 username
    String username = jwtUtils.getUsernameFromToken(token);

    // 2. 從 DB 查出 User 並回傳 ID
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found for username: " + username));

    return user.getId();
  }

  // 1. 取得使用者的所有願望商品
  public List<Product> getUserWishlist(String token) {
    Long userId = getUserIdFromToken(token);
    List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
    return wishlists.stream()
        .map(Wishlist::getProduct)
        .collect(Collectors.toList());
  }

  // 2. 切換新增 / 刪除收藏
  @Transactional
  public void toggleWishlist(String token, Long productId) {
    Long userId = getUserIdFromToken(token);
    Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);

    if (existing.isPresent()) {
      wishlistRepository.delete(existing.get());
    } else {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
      wishlistRepository.save(new Wishlist(userId, product));
    }
  }

  // 3. 清空特定使用者的願望清單 (Clear All)
  @Transactional
  public void clearUserWishlist(String token) {
    Long userId = getUserIdFromToken(token);
    wishlistRepository.deleteByUserId(userId);
  }
}