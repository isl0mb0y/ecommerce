package com.shop.catalog.domain;

import jakarta.persistence.*;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private long priceInCents;

    private int stock;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    protected Product() {
    }

    public Product(String name, long priceInCents, int stock) {
        this.name = name;
        this.priceInCents = priceInCents;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPriceInCents() {
        return priceInCents;
    }

    public int getStock() {
        return stock;
    }

    public void setPriceInCents(long priceInCents) {
        this.priceInCents = priceInCents;
    }
}
