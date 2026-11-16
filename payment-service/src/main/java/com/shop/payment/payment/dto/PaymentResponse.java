package com.shop.payment.payment.dto;

import java.time.Instant;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.shop.payment.payment.Payment;
import com.shop.payment.payment.PaymentStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentResponse(Long id, Long orderId, long amountInCents, PaymentStatus status, Instant createdAt) {

    static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmountInCents(),
                payment.getStatus(), payment.getCreatedAt());
    }
}
