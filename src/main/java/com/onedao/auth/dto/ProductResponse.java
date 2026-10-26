package com.onedao.auth.dto;

import com.onedao.auth.model.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(Long id, String name, String description, BigDecimal price,
                              Integer quantity, Instant createdAt, Instant updatedAt) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getQuantity(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
