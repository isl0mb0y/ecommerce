package com.shop.order.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.shop.order.catalog.CatalogClient;
import com.shop.order.catalog.ProductResponse;
import com.shop.order.messaging.RabbitConfig;
import com.shop.order.messaging.event.OrderCreated;
import com.shop.order.order.dto.CreateOrderRequest;
import com.shop.order.order.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Not transactional: the order must be committed before OrderCreated is published.
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

        Order saved = orderRepository.save(order);

        OrderCreated event = OrderCreated.from(saved);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ORDER_CREATED_KEY, event);
        log.info("Published {}: {}", RabbitConfig.ORDER_CREATED_KEY, event);

        return OrderResponse.from(saved);
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
