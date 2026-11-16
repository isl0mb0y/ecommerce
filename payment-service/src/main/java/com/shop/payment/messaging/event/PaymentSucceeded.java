package com.shop.payment.messaging.event;

import java.util.List;

public record PaymentSucceeded(String eventId, Long orderId, Long paymentId, List<Item> items) {

    public record Item(Long productId, int quantity) {
    }
}
