package com.shop.catalog.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stock_reservations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "product_id"}))
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.RESERVED;

    private Instant createdAt;

    protected StockReservation() {

    }

    public StockReservation(Long orderId, Long productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = Instant.now();
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
