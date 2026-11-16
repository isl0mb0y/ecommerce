package com.shop.payment.api.rest.dto;

import java.time.Instant;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.shop.payment.domain.Payment;
import com.shop.payment.domain.PaymentStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentResponse(Long id, Long orderId, long amountInCents, PaymentStatus status, Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmountInCents(),
                payment.getStatus(), payment.getCreatedAt());
    }
}
