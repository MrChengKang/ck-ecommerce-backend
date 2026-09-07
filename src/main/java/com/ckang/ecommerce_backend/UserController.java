package com.ckang.ecommerce_backend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private OrderRepository orderRepository;

  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    // authentication.getName() 會拿 JwtAuthenticationFilter 傳進去的 username (或 email)
    String identifier = authentication.getName();

    // 根據你的 UserRepository 查詢方式 (可以用 findByUsername 或 findByEmail)
    Optional<User> userOpt = userRepository.findByUsername(identifier);

    if (userOpt.isPresent()) {
      User user = userOpt.get();
      // 安全起見，清空密碼再回傳
      user.setPassword(null);
      return ResponseEntity.ok(user);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in database");
    }
  }

  @PutMapping("/me")
  public ResponseEntity<?> updateCurrentUser(
      Authentication authentication,
      @RequestBody java.util.Map<String, Object> updates) {

    if (authentication == null || authentication.getName() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    String identifier = authentication.getName();

    // 尋找當前登入使用者
    Optional<User> userOpt = userRepository.findByUsername(identifier);

    if (userOpt.isPresent()) {
      User existingUser = userOpt.get();

      // 💡 兼容前端可能傳過來的不同 Key (phoneNo, contactNumber, phone)
      if (updates.containsKey("phoneNo") && updates.get("phoneNo") != null) {
        existingUser.setPhoneNo((String) updates.get("phoneNo"));
      } else if (updates.containsKey("contactNumber") && updates.get("contactNumber") != null) {
        existingUser.setPhoneNo((String) updates.get("contactNumber"));
      } else if (updates.containsKey("phone") && updates.get("phone") != null) {
        existingUser.setPhoneNo((String) updates.get("phone"));
      }

      // 💡 其它欄位更新
      if (updates.containsKey("address") && updates.get("address") != null) {
        existingUser.setAddress((String) updates.get("address"));
      }
      if (updates.containsKey("email") && updates.get("email") != null) {
        existingUser.setEmail((String) updates.get("email"));
      }
      if (updates.containsKey("profilePic") && updates.get("profilePic") != null) {
        existingUser.setProfilePic((String) updates.get("profilePic"));
      }

      // 儲存至資料庫
      User savedUser = userRepository.save(existingUser);
      savedUser.setPassword(null); // 清除密碼敏感資訊

      return ResponseEntity.ok(savedUser);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }
  }

  @PostMapping("/me/upload-avatar")
  public ResponseEntity<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登入");
    }

    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    try {
      Path uploadDir = Paths.get("./uploads");
      if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
      }

      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }

      String savedFilename = UUID.randomUUID().toString() + extension;
      Path filePath = uploadDir.resolve(savedFilename);
      Files.copy(file.getInputStream(), filePath);

      String currentUserIdentifier = authentication.getName();
      User user = userRepository.findByEmail(currentUserIdentifier)
          .orElseThrow(() -> new RuntimeException("User not found"));

      String fileUrl = "http://localhost:8080/uploads/" + savedFilename;
      user.setProfilePic(fileUrl);
      userRepository.save(user);

      return ResponseEntity.ok(Map.of("url", fileUrl));

    } catch (IOException e) {
      return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
    }
  }

  @GetMapping
  public ResponseEntity<?> getAllCustomers() {
    List<User> users = userRepository.findByRole("USER");

    List<CustomerDTO> dtos = users.stream().map(user -> {
      CustomerDTO dto = new CustomerDTO();
      dto.setId(user.getId());
      dto.setUsername(user.getUsername());
      dto.setEmail(user.getEmail());

      Long count = orderRepository.countByCustomerEmail(user.getEmail());
      Double spent = orderRepository.sumTotalAmountByCustomerEmail(user.getEmail());

      dto.setTotalOrders(count != null ? count : 0L);
      dto.setTotalSpent(spent != null ? spent : 0.0);

      return dto;
    }).collect(Collectors.toList());

    return ResponseEntity.ok(dtos);
  }
}