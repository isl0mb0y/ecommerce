package com.shop.payment.messaging.event;

public record StockReserved(String eventId, Long orderId, long totalInCents) {
}
