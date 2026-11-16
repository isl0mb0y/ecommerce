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

    protected OutboxEvent() {

    }

    public OutboxEvent(String routingKey, String payload) {
        this.routingKey = routingKey;
        this.payload = payload;
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

    public String getPayload() {
        return payload;
    }
}
