package com.ckang.ecommerce_backend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    String identifier = authentication.getName();

    Optional<User> userOpt = userRepository.findByUsername(identifier);

    if (userOpt.isPresent()) {
      User user = userOpt.get();
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

    Optional<User> userOpt = userRepository.findByUsername(identifier);

    if (userOpt.isPresent()) {
      User existingUser = userOpt.get();

      // 💡 加上 username 的更新判斷（兼顧 name 欄位）
      if (updates.containsKey("username") && updates.get("username") != null) {
        existingUser.setUsername((String) updates.get("username"));
      } else if (updates.containsKey("name") && updates.get("name") != null) {
        existingUser.setUsername((String) updates.get("name"));
      }

      if (updates.containsKey("phoneNo") && updates.get("phoneNo") != null) {
        existingUser.setPhoneNo((String) updates.get("phoneNo"));
      } else if (updates.containsKey("contactNumber") && updates.get("contactNumber") != null) {
        existingUser.setPhoneNo((String) updates.get("contactNumber"));
      } else if (updates.containsKey("phone") && updates.get("phone") != null) {
        existingUser.setPhoneNo((String) updates.get("phone"));
      }

      if (updates.containsKey("address") && updates.get("address") != null) {
        existingUser.setAddress((String) updates.get("address"));
      }
      if (updates.containsKey("email") && updates.get("email") != null) {
        existingUser.setEmail((String) updates.get("email"));
      }
      if (updates.containsKey("profilePic") && updates.get("profilePic") != null) {
        existingUser.setProfilePic((String) updates.get("profilePic"));
      }

      User savedUser = userRepository.save(existingUser);
      savedUser.setPassword(null);

      return ResponseEntity.ok(savedUser);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }
  }

  @PostMapping("/me/upload-avatar")
  public ResponseEntity<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    try {
      String identifier = authentication.getName();
      System.out.println(">>> Auth User: " + identifier);
      System.out.println(">>> File received: " + file.getOriginalFilename());

      User user = userRepository.findByUsername(identifier)
          .orElseGet(() -> userRepository.findByEmail(identifier).orElse(null));

      if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + identifier);
      }

      Path uploadDir = Paths.get("./uploads").toAbsolutePath().normalize();
      if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
      }

      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }

      String savedFilename = UUID.randomUUID().toString() + extension;
      Path targetPath = uploadDir.resolve(savedFilename);

      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

      String fileUrl = "/uploads/" + savedFilename;
      user.setProfilePic(fileUrl);
      userRepository.save(user);

      System.out.println(">>> Saved successfully at: " + fileUrl);
      return ResponseEntity.ok(Map.of("url", fileUrl));

    } catch (Throwable e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Upload error: " + e.getMessage());
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

  @PutMapping("/me/password")
  public ResponseEntity<?> changePassword(
      Authentication authentication,
      @RequestBody Map<String, String> requestBody) {

    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }

    String currentPassword = requestBody.get("currentPassword");
    String newPassword = requestBody.get("newPassword");

    if (currentPassword == null || newPassword == null || newPassword.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("Passwords cannot be empty");
    }

    String identifier = authentication.getName();
    User user = userRepository.findByUsername(identifier)
        .orElseGet(() -> userRepository.findByEmail(identifier).orElse(null));

    if (user == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    // 1. 驗證舊密碼是否正確
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      return ResponseEntity.badRequest().body("Current password incorrect");
    }

    // 2. 加密新密碼並存檔
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
  }
}