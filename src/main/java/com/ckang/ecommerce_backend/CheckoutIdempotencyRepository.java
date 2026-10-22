package com.ckang.ecommerce_backend;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CheckoutIdempotencyRepository extends JpaRepository<CheckoutIdempotency, Long> {

  @Modifying
  @Query(value = "INSERT INTO checkout_idempotency "
      + "(customer_id, idempotency_key, request_hash) "
      + "VALUES (:customerId, :idempotencyKey, :requestHash) "
      + "ON DUPLICATE KEY UPDATE id = id", nativeQuery = true)
  int reserveKey(
      @Param("customerId") Long customerId,
      @Param("idempotencyKey") String idempotencyKey,
      @Param("requestHash") String requestHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM CheckoutIdempotency c "
      + "WHERE c.customerId = :customerId AND c.idempotencyKey = :idempotencyKey")
  Optional<CheckoutIdempotency> findForUpdate(
      @Param("customerId") Long customerId,
      @Param("idempotencyKey") String idempotencyKey);
}
