package com.shop.order.order;

public record PaymentFailed(Long orderId, String reason) {
}
