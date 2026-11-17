package com.shop.catalog.messaging.event;

public record StockRejected(String eventId, Long orderId, String reason) {
}
