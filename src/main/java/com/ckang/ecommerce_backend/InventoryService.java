package com.ckang.ecommerce_backend;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class InventoryService {

  private final ProductRepository productRepository;

  public InventoryService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public boolean restoreInventory(Order order) {
    Map<Long, Long> quantityByProduct = new HashMap<>();

    if (order.getItems() == null || order.getItems().isEmpty()) {
      return true;
    }

    for (OrderItem item : order.getItems()) {
      if (item == null || item.getProductId() == null ||
          item.getQuantity() == null || item.getQuantity() <= 0) {
        return false;
      }

      long combined = quantityByProduct.getOrDefault(item.getProductId(), 0L) + item.getQuantity();
      if (combined > Integer.MAX_VALUE) {
        return false;
      }
      quantityByProduct.put(item.getProductId(), combined);
    }

    Map<Long, Product> lockedProducts = new LinkedHashMap<>();
    List<Long> productIds = quantityByProduct.keySet().stream().sorted().toList();

    for (Long productId : productIds) {
      Product product = productRepository.findByIdForUpdate(productId).orElse(null);
      if (product == null || product.getStockQuantity() == null ||
          (long) product.getStockQuantity() + quantityByProduct.get(productId) > Integer.MAX_VALUE) {
        return false;
      }
      lockedProducts.put(productId, product);
    }

    for (Long productId : productIds) {
      Product product = lockedProducts.get(productId);
      product.setStockQuantity((int) (product.getStockQuantity() + quantityByProduct.get(productId)));
    }
    return true;
  }
}
