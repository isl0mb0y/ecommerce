package com.shop.catalog.messaging.event;

public record StockReserved (String eventId, Long orderId, long totalInCents) {
}
