package com.shop.order.messaging.event;

public record PaymentFailed(Long orderId, String reason) {
}
