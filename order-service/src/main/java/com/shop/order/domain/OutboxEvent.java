package com.shop.order.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routingKey;

    @Column(columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status = OutboxStatus.PENDING;

    private Instant createdAt;

    private Instant publishedAt;

    private String requestId;

    private String messageKey;

    protected OutboxEvent() {

    }

    public OutboxEvent(String routingKey, String messageKey, String payload, String requestId) {
        this.routingKey = routingKey;
        this.messageKey = messageKey;
        this.payload = payload;
        this.requestId = requestId;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public String getRequestId() {
        return requestId;
    }
}
