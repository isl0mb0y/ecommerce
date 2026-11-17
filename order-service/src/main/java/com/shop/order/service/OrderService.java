package com.shop.order.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.client.catalog.CatalogClient;
import com.shop.order.client.catalog.dto.ProductResponse;
import com.shop.order.domain.Order;
import com.shop.order.domain.OutboxEvent;
import com.shop.order.filter.RequestIdFilter;
import com.shop.order.messaging.config.RabbitConfig;
import com.shop.order.messaging.event.OrderCreated;
import com.shop.order.api.rest.dto.CreateOrderRequest;
import com.shop.order.api.rest.dto.OrderResponse;
import com.shop.order.repository.OrderRepository;
import com.shop.order.domain.OrderStatus;
import com.shop.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient , OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    // Catalog calls stay outside the transaction; order and outbox row are saved atomically.
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        Order order = new Order(userId);
        mergeQuantities(request).forEach((productId, quantity) -> {
            ProductResponse product = catalogClient.findProduct(productId)
                    .orElseThrow(() -> badRequest("Product " + productId + " not found"));
            if (product.stock() < quantity) {
                throw badRequest("Product " + productId + " is out of stock");
            }
            order.addItem(productId, quantity, product.priceInCents());
        });

        Order saved = transactionTemplate.execute(status -> {
            Order persisted = orderRepository.save(order);
            OrderCreated event = OrderCreated.from(persisted);
            outboxEventRepository.save(new OutboxEvent(RabbitConfig.ORDER_CREATED_KEY, toJson(event), MDC.get(RequestIdFilter.MDC_KEY)));
            return persisted;
        });

        return OrderResponse.from(saved);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public void updateStatus(Long orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> order.setStatus(status),
                () -> log.warn("Order {} not found, cannot set status {}", orderId, status));
    }

    // Lines for the same product are merged so the stock check sees the full quantity.
    private Map<Long, Integer> mergeQuantities(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw badRequest("items must not be empty");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CreateOrderRequest.ItemRequest item : request.items()) {
            if (item.productId() == null || item.quantity() == null || item.quantity() <= 0) {
                throw badRequest("each item needs a productId and a positive quantity");
            }
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
