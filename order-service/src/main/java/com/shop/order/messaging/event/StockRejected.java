package com.shop.order.messaging.event;

public record StockRejected(String eventId, Long orderId, String reason) {
}
