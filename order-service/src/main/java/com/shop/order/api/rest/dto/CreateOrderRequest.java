package com.shop.order.api.rest.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public record CreateOrderRequest(List<ItemRequest> items) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ItemRequest(Long productId, Integer quantity) {
    }
}
