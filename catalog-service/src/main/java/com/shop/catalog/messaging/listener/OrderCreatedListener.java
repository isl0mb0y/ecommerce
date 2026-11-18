package com.shop.catalog.messaging.listener;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.shop.catalog.messaging.config.KafkaConfig;
import com.shop.catalog.messaging.event.OrderCreated;
import com.shop.catalog.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final StockReservationService stockReservationService;

    public OrderCreatedListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC)
    public void onOrderCreated(OrderCreated event) {
        log.info("Consumed {}: {}", KafkaConfig.ORDER_CREATED_TOPIC, event, kv("orderId", event.orderId()));
        stockReservationService.reserve(event);
    }
}
