package com.shop.payment.messaging.event;

public record PaymentFailed(Long orderId, String reason) {
}
