package com.shop.catalog.api.rest.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.shop.catalog.domain.Product;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductResponse(Long id, String name, long priceInCents, int stock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPriceInCents(), product.getStock());
    }
}
