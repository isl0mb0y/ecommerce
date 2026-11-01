package com.shop.catalog.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private long priceInCents;

    private int stock;

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
}
