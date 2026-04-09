package com.ckang.ecommerce_backend;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private JavaMailSender mailSender;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
  private String fromEmail;

  private java.util.Map<String, String> otpStorage = new java.util.HashMap<>();

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    try {
      String identifier = req.getUsername();
      System.out.println("--- 收到登入請求: " + identifier + " ---");

      return userRepository.findByUsername(identifier)
          .or(() -> userRepository.findAll().stream()
              .filter(u -> identifier.equalsIgnoreCase(u.getEmail()))
              .findFirst())
          .map(user -> {
            if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
              String token = jwtUtils.generateToken(user.getUsername());

              java.util.Map<String, Object> response = new java.util.HashMap<>();
              response.put("token", token);
              response.put("role", user.getRole() == null ? "USER" : user.getRole());
              response.put("id", user.getId());
              response.put("email", user.getEmail());
              response.put("username", user.getUsername());

              System.out.println("✅ 登入成功: " + user.getUsername());
              return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body("INVALID_CREDENTIALS");
          })
          .orElse(ResponseEntity.status(401).body("INVALID_CREDENTIALS"));

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).body("SERVER_ERROR");
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody java.util.Map<String, String> regData) {
    try {
      String username = regData.get("username");
      String password = regData.get("password");
      String email = regData.get("email");

      if (password.length() < 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")) {
        return ResponseEntity.badRequest().body("PASSWORD_TOO_WEAK");
      }

      if (!email.contains("@") || !email.contains(".")) {
        return ResponseEntity.badRequest().body("INVALID_EMAIL_FORMAT");
      }

      // 🔍 1. 檢查 Username 是否已存在 (你原本已有的)
      if (userRepository.findByUsername(username).isPresent()) {
        return ResponseEntity.badRequest().body("USERNAME_ALREADY_EXISTS");
      }

      // 🔍 2. 增加：檢查 Email 是否已存在 (防止觸發資料庫 Duplicate Entry)
      boolean emailExists = userRepository.findAll().stream()
          .anyMatch(user -> email.equalsIgnoreCase(user.getEmail()));
      if (emailExists) {
        return ResponseEntity.badRequest().body("EMAIL_ALREADY_REGISTERED");
      }

      User newUser = new User();
      newUser.setUsername(username);
      String encodedPassword = passwordEncoder.encode(regData.get("password"));
      newUser.setPassword(encodedPassword);
      newUser.setEmail(email);
      newUser.setRole("USER");

      userRepository.save(newUser);
      return ResponseEntity.ok("REGISTRATION_SUCCESS");

    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      return ResponseEntity.badRequest().body("ACCOUNT_DATA_CONFLICT");
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).body("SERVER_ERROR");
    }
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> req) {
    String email = req.get("email").trim().toLowerCase();

    return userRepository.findAll().stream()
        .filter(user -> user.getEmail() != null && user.getEmail().trim().toLowerCase().equals(email))
        .findFirst()
        .map(user -> {
          String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
          otpStorage.put(email, otp);

          // 📧 執行真實發信邏輯
          try {
            sendOtpEmail(email, otp);
            return ResponseEntity.ok("OTP has been sent to your email!");
          } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
          }
        })
        .orElse(ResponseEntity.status(404).body("Email not found!"));
  }

  private void sendOtpEmail(String toEmail, String otp) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("[CK STORE] Password Reset Verification Code");
    message.setText("Hello! Your verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");
    mailSender.send(message);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> req) {
    String email = req.get("email").trim().toLowerCase();
    String otp = req.get("otp");
    String newPassword = req.get("newPassword").trim();

    if (otpStorage.containsKey(email) && otpStorage.get(email).equals(otp)) {
      return userRepository.findAll().stream()
          .filter(user -> user.getEmail() != null && email.equalsIgnoreCase(user.getEmail().trim()))
          .findFirst()
          .map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            otpStorage.remove(email); // 使用完刪除
            return ResponseEntity.ok("Password reset successfully!");
          })
          .orElse(ResponseEntity.status(404).body("User not found"));
    }
    return ResponseEntity.status(400).body("Invalid OTP!");
  }

  @PostMapping("/validate-token")
  public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
    try {
      // 去掉 "Bearer " 字眼拿到純 Token
      String token = authHeader.substring(7);

      // 🚨 關鍵：呼叫剛才寫的 validateToken 方法
      // 這個方法裡面 parseClaimsJws 如果過期，會噴 ExpiredJwtException
      boolean isValid = jwtUtils.validateToken(token);

      if (isValid) {
        return ResponseEntity.ok("ALIVE");
      } else {
        return ResponseEntity.status(401).body("EXPIRED");
      }
    } catch (Exception e) {
      return ResponseEntity.status(401).body("INVALID_OR_EXPIRED");
    }
  }
}