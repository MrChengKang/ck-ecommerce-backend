package com.ckang.ecommerce_backend;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class BillplzService {

  private final RestClient restClient;
  private final String secretKey;
  private final String collectionId;
  private final String xSignatureKey;

  public BillplzService(
      RestClient.Builder builder,
      @Value("${billplz.base-url}") String baseUrl,
      @Value("${billplz.secret-key}") String secretKey,
      @Value("${billplz.collection-id}") String collectionId,
      @Value("${billplz.x-signature-key}") String xSignatureKey) {
    this.restClient = builder
        .baseUrl(baseUrl)
        .defaultHeaders(headers -> headers.setBasicAuth(secretKey, ""))
        .build();
    this.secretKey = secretKey;
    this.xSignatureKey = xSignatureKey;
    this.collectionId = collectionId;
  }

  public BillplzBill createBill(
      Order order,
      String callbackUrl,
      String redirectUrl) {
    if (secretKey == null || secretKey.isBlank()
        || collectionId == null || collectionId.isBlank()) {
      throw new IllegalStateException("Billplz Sandbox settings are missing");
    }
    if (order == null || order.getTotalAmount() == null) {
      throw new IllegalArgumentException("Order amount is missing");
    }

    long amountInSen = BigDecimal.valueOf(order.getTotalAmount())
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("collection_id", collectionId);
    form.add("email", order.getCustomerEmail());
    form.add("name", order.getCustomerName());
    form.add("amount", Long.toString(amountInSen));
    form.add("description", "Order " + order.getOrderNumber());
    form.add("callback_url", callbackUrl);
    form.add("redirect_url", redirectUrl);

    BillplzBill bill = restClient.post()
        .uri("v3/bills")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(BillplzBill.class);

    if (bill == null || bill.url() == null || bill.url().isBlank()) {
      throw new IllegalStateException("Billplz did not return a payment URL");
    }

    return bill;
  }

  public boolean deleteUnpaidBill(String billId) {
    if (billId == null || billId.isBlank()) {
      return true;
    }
    try {
      restClient.delete()
          .uri("v3/bills/{billId}", billId)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (HttpClientErrorException e) {
      // A missing bill is already unavailable. A 422 typically means it was paid,
      // so keep its stock reserved and let the payment callback reconcile it.
      return e.getStatusCode().value() == 404;
    } catch (RestClientException e) {
      return false;
    }
  }

  public BillplzBill getBill(String billId) {
    if (billId == null || billId.isBlank()) {
      throw new IllegalArgumentException("Billplz bill ID is required");
    }
    return restClient.get()
        .uri("v3/bills/{billId}", billId)
        .retrieve()
        .body(BillplzBill.class);
  }

  public boolean isValidCallbackSignature(Map<String, String> params) {
    if (params == null || xSignatureKey == null || xSignatureKey.isBlank()) {
      return false;
    }

    String receivedSignature = params.get("x_signature");
    if (receivedSignature == null || receivedSignature.isBlank()) {
      return false;
    }

    List<String> sourceParts = new ArrayList<>();
    params.forEach((key, value) -> {
      if (!"x_signature".equalsIgnoreCase(key)) {
        sourceParts.add(key + (value == null ? "" : value));
      }
    });

    sourceParts.sort(String.CASE_INSENSITIVE_ORDER);
    String source = String.join("|", sourceParts);

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(
          xSignatureKey.getBytes(StandardCharsets.UTF_8),
          "HmacSHA256"));

      String expectedSignature = java.util.HexFormat.of()
          .formatHex(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));

      return MessageDigest.isEqual(
          expectedSignature.toLowerCase(java.util.Locale.ROOT)
              .getBytes(StandardCharsets.UTF_8),
          receivedSignature.toLowerCase(java.util.Locale.ROOT)
              .getBytes(StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Could not verify Billplz signature", e);
    }
  }

  public record BillplzBill(
      String id,
      String url,
      Boolean paid,
      String state,
      String collection_id,
      Long amount) {
  }
}
