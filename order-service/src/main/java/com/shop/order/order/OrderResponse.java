package com.shop.order.order;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderResponse(Long id, Long userId, OrderStatus status, long totalInCents, Instant createdAt,
                            List<ItemResponse> items) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ItemResponse(Long id, Long productId, int quantity, long priceInCents) {
    }

    static OrderResponse from(Order order) {
        List<ItemResponse> items = order.getItems().stream()
                .map(item -> new ItemResponse(item.getId(), item.getProductId(), item.getQuantity(),
                        item.getPriceInCents()))
                .toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(), order.getTotalInCents(),
                order.getCreatedAt(), items);
    }
}
