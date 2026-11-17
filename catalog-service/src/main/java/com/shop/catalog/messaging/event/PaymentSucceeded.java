package com.shop.catalog.messaging.event;

public record PaymentSucceeded(String eventId, Long orderId, Long paymentId) {
}
