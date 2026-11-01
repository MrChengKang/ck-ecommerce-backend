package com.ckang.ecommerce_backend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Sends transactional email through Resend's HTTPS API (works on Render Free). */
@Service
public class EmailApiService {
  private static final URI RESEND_EMAILS_URI = URI.create("https://api.resend.com/emails");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String fromEmail;

  public EmailApiService(
      ObjectMapper objectMapper,
      @Value("${resend.api-key:}") String apiKey,
      @Value("${email.from:onboarding@resend.dev}") String fromEmail) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.fromEmail = fromEmail;
  }

  public void sendText(String to, String subject, String text) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("RESEND_API_KEY is not configured");
    }

    try {
      String payload = objectMapper.writeValueAsString(Map.of(
          "from", fromEmail,
          "to", List.of(to),
          "subject", subject,
          "text", text));

      HttpRequest request = HttpRequest.newBuilder(RESEND_EMAILS_URI)
          .timeout(Duration.ofSeconds(15))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Resend API returned HTTP " + response.statusCode() + ": " + response.body());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while sending email through Resend", e);
    } catch (IOException e) {
      throw new IllegalStateException("Could not send email through Resend API", e);
    }
  }
}
