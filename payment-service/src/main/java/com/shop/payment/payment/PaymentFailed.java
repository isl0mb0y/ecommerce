package com.shop.payment.payment;

public record PaymentFailed(Long orderId, String reason) {
}
