package com.shop.catalog.messaging.listener;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.shop.catalog.messaging.config.KafkaConfig;
import com.shop.catalog.messaging.event.PaymentSucceeded;
import com.shop.catalog.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentSucceededListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededListener.class);

    private final StockReservationService stockReservationService;

    public PaymentSucceededListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }


    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCEEDED_TOPIC)
    public void onPaymentSucceeded(PaymentSucceeded event) {
        log.info("Consumed {}: {}", KafkaConfig.PAYMENT_SUCCEEDED_TOPIC, event, kv("orderId", event.orderId()));
        stockReservationService.confirm(event.orderId());
    }
}
