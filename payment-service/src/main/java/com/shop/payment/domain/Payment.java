package com.shop.payment.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long orderId;

    private long amountInCents;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant createdAt;

    protected Payment() {
    }

    public Payment(Long orderId, long amountInCents, PaymentStatus status) {
        this.orderId = orderId;
        this.amountInCents = amountInCents;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public long getAmountInCents() {
        return amountInCents;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
