package com.shop.order.messaging.event;

import com.shop.order.order.Order;

import java.util.List;

public record OrderCreated(Long orderId, Long userId, long totalInCents, List<Item> items) {

    public record Item(Long productId, int quantity) {
    }

    public static OrderCreated from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(item.getProductId(), item.getQuantity()))
                .toList();
        return new OrderCreated(order.getId(), order.getUserId(), order.getTotalInCents(), items);
    }
}
