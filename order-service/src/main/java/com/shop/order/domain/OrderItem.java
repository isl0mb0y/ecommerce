package com.shop.order.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;

    private int quantity;

    private long priceInCents;

    protected OrderItem() {
    }

    OrderItem(Order order, Long productId, int quantity, long priceInCents) {
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.priceInCents = priceInCents;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPriceInCents() {
        return priceInCents;
    }
}
