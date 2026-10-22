package com.ckang.ecommerce_backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
    "jwt.secret=integration-test-secret-key-that-is-long-enough-32-bytes",
    "spring.mail.username=test@example.test",
    "spring.mail.password=test-only-not-a-real-password",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers(disabledWithoutDocker = true)
class InventoryConcurrencyIntegrationTest {

  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired
  private OrderController orderController;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserRepository userRepository;

  private Product product;
  private UsernamePasswordAuthenticationToken authentication;

  @BeforeEach
  void prepareOneUnitOfStock() {
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    User user = new User();
    user.setUsername("concurrency-test-user");
    user.setPassword("not-used-in-this-test");
    user.setEmail("concurrency-test@example.test");
    user.setRole("USER");
    userRepository.saveAndFlush(user);

    product = new Product();
    product.setName("Concurrency test product");
    product.setPrice(new BigDecimal("10.00"));
    product.setStockQuantity(1);
    product = productRepository.saveAndFlush(product);

    authentication = new UsernamePasswordAuthenticationToken(
        user.getUsername(), "unused");
  }

  @Test
  void simultaneousCheckoutsCannotOversellLastUnit() throws Exception {
    CheckoutRequest request = new CheckoutRequest(
        "Concurrency Test",
        "Test address",
        "TEST",
        List.of(new CheckoutItemRequest(product.getId(), 1)));

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
      Future<ResponseEntity<?>> first = workers.submit(
          () -> checkoutTogether(request, ready, start, UUID.randomUUID().toString()));
      Future<ResponseEntity<?>> second = workers.submit(
          () -> checkoutTogether(request, ready, start, UUID.randomUUID().toString()));

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      List<Integer> statuses = List.of(
          first.get(30, TimeUnit.SECONDS).getStatusCode().value(),
          second.get(30, TimeUnit.SECONDS).getStatusCode().value());

      assertThat(statuses).containsExactlyInAnyOrder(200, 400);
    } finally {
      start.countDown();
    }

    assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
        .isZero();
    assertThat(orderRepository.count()).isEqualTo(1);
  }

  @Test
  void simultaneousRetriesWithSameKeyReturnOneOrderAndDeductStockOnce() throws Exception {
    CheckoutRequest request = new CheckoutRequest(
        "Idempotency Test",
        "Test address",
        "TEST",
        List.of(new CheckoutItemRequest(product.getId(), 1)));
    String idempotencyKey = UUID.randomUUID().toString();

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
      Future<ResponseEntity<?>> first = workers.submit(
          () -> checkoutTogether(request, ready, start, idempotencyKey));
      Future<ResponseEntity<?>> second = workers.submit(
          () -> checkoutTogether(request, ready, start, idempotencyKey));

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      ResponseEntity<?> firstResponse = first.get(30, TimeUnit.SECONDS);
      ResponseEntity<?> secondResponse = second.get(30, TimeUnit.SECONDS);
      assertThat(firstResponse.getStatusCode().value()).isEqualTo(200);
      assertThat(secondResponse.getStatusCode().value()).isEqualTo(200);
      assertThat(((Order) firstResponse.getBody()).getId())
          .isEqualTo(((Order) secondResponse.getBody()).getId());
    } finally {
      start.countDown();
    }

    assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
        .isZero();
    assertThat(orderRepository.count()).isEqualTo(1);
  }

  @Test
  void reusingKeyWithDifferentRequestReturnsConflict() {
    String idempotencyKey = UUID.randomUUID().toString();
    CheckoutRequest original = new CheckoutRequest(
        "Idempotency Test",
        "Test address",
        "TEST",
        List.of(new CheckoutItemRequest(product.getId(), 1)));
    CheckoutRequest changed = new CheckoutRequest(
        "Different Recipient",
        "Test address",
        "TEST",
        List.of(new CheckoutItemRequest(product.getId(), 1)));

    ResponseEntity<?> first = orderController.checkout(original, idempotencyKey, authentication);
    ResponseEntity<?> retryWithChangedBody = orderController.checkout(changed, idempotencyKey, authentication);

    assertThat(first.getStatusCode().value()).isEqualTo(200);
    assertThat(retryWithChangedBody.getStatusCode().value()).isEqualTo(409);
    assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
        .isZero();
    assertThat(orderRepository.count()).isEqualTo(1);
  }

  private ResponseEntity<?> checkoutTogether(
      CheckoutRequest request,
      CountDownLatch ready,
      CountDownLatch start,
      String idempotencyKey) throws InterruptedException {
    ready.countDown();
    if (!start.await(10, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Timed out waiting for concurrent checkout start");
    }
    return orderController.checkout(request, idempotencyKey, authentication);
  }
}
