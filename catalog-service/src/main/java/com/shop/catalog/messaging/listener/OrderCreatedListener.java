package com.shop.catalog.messaging.listener;

import com.shop.catalog.messaging.config.RabbitConfig;
import com.shop.catalog.messaging.event.OrderCreated;
import com.shop.catalog.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final StockReservationService stockReservationService;

    public OrderCreatedListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreated event) {
        log.info("Consumed {}: {}", RabbitConfig.ORDER_CREATED_KEY, event);
        stockReservationService.reserve(event);
    }
}
