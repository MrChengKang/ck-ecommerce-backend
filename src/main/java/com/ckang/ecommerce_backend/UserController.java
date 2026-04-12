package com.ckang.ecommerce_backend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private OrderRepository orderRepository;

  @GetMapping("/{id}")
  public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
    return userRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // 💡 更新用戶資料
  @PutMapping("/{id}")
  public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody User updatedData) {
    return userRepository.findById(id).map(user -> {
      user.setUsername(updatedData.getUsername());
      user.setEmail(updatedData.getEmail());
      user.setAddress(updatedData.getAddress());
      user.setPhoneNo(updatedData.getPhoneNo());
      user.setProfilePic(updatedData.getProfilePic());

      userRepository.save(user);
      return ResponseEntity.ok("Profile updated successfully!");
    }).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/upload-avatar")
  public ResponseEntity<?> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    try {
      // 1. 確保 uploads 資料夾存在
      Path uploadDir = Paths.get("./uploads");
      if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
      }

      // 2. 產生存儲的檔名 (使用 UUID 防止檔名重複)
      String originalFilename = file.getOriginalFilename();
      String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      String savedFilename = UUID.randomUUID().toString() + extension;

      // 3. 儲存檔案到本地
      Path filePath = uploadDir.resolve(savedFilename);
      Files.copy(file.getInputStream(), filePath);

      // 4. 更新用戶資料庫裡的 profilePic 欄位 (存入 URL 路徑)
      User user = userRepository.findById(id).orElseThrow();
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