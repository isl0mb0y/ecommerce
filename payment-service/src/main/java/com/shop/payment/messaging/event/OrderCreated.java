package com.shop.payment.messaging.event;

import java.util.List;

public record OrderCreated(Long orderId, Long userId, long totalInCents, List<Item> items) {

    public record Item(Long productId, int quantity) {
    }
}
