package com.shop.order.messaging.event;

public record PaymentSucceeded(Long orderId, Long paymentId) {
}
