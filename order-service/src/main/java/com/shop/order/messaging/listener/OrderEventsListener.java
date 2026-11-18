package com.shop.order.messaging.listener;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.shop.order.messaging.config.KafkaConfig;
import com.shop.order.messaging.event.PaymentFailed;
import com.shop.order.messaging.event.PaymentSucceeded;
import com.shop.order.messaging.event.StockRejected;
import com.shop.order.service.OrderService;
import com.shop.order.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final OrderService orderService;

    public OrderEventsListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCEEDED_TOPIC)
    public void onPaymentSucceeded(PaymentSucceeded event) {
        log.info("Consumed {}: {}", KafkaConfig.PAYMENT_SUCCEEDED_TOPIC, event, kv("orderId", event.orderId()));
        orderService.updateStatus(event.orderId(), OrderStatus.PAID);
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC)
    public void onPaymentFailed(PaymentFailed event) {
        log.info("Consumed {}: {}", KafkaConfig.PAYMENT_FAILED_TOPIC, event, kv("orderId", event.orderId()));
        orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED);
    }

    @KafkaListener(topics = KafkaConfig.STOCK_REJECTED_TOPIC)
    public void onStockRejected(StockRejected event) {
        log.info("Consumed {}: {}", KafkaConfig.STOCK_REJECTED_TOPIC, event, kv("orderId", event.orderId()));
        orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED);
    }
}
