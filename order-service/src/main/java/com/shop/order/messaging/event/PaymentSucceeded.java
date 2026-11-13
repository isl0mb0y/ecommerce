package com.shop.order.order;

import java.util.List;

public record PaymentSucceeded(Long orderId, Long paymentId, List<Item> items) {

    public record Item(Long productId, int quantity) {
    }
}
