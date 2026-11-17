package com.shop.catalog.messaging.event;

public record PaymentFailed(Long orderId, String reason) {
}
