package com.ckang.ecommerce_backend;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtils jwtUtils;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    try {
      System.out.println("--- 收到登入請求: " + req.getUsername() + " ---");

      return userRepository.findByUsername(req.getUsername())
          .map(user -> {
            // 🔍 增加偵錯印出
            System.out.println("資料庫匹配成功，Role 為: " + user.getRole());

            if (user.getPassword().equals(req.getPassword())) {
              String token = jwtUtils.generateToken(user.getUsername());

              java.util.Map<String, String> response = new java.util.HashMap<>();
              response.put("token", token);

              // 🚨 防呆：如果 role 是 null，給它一個預設值 "USER"
              String role = (user.getRole() == null) ? "USER" : user.getRole();
              response.put("role", role);

              System.out.println("✅ 登入成功，準備回傳 Token");
              return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body("Password wrong");
          })
          .orElse(ResponseEntity.status(401).body("User not found"));

    } catch (Exception e) {
      // 🚨 這裡會幫你抓出到底是哪一行崩潰
      System.err.println("❌ 伺服器內部崩潰！原因: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(500).body("Internal Error: " + e.getMessage());
    }
  }
}