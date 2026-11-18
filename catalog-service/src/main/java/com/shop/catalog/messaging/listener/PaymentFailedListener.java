package com.shop.catalog.messaging.listener;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.shop.catalog.messaging.config.KafkaConfig;
import com.shop.catalog.messaging.event.PaymentFailed;
import com.shop.catalog.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedListener.class);

    private final StockReservationService stockReservationService;

    public PaymentFailedListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC)
    public void onPaymentFailed(PaymentFailed event) {
        log.info("Consumed {}: {}", KafkaConfig.PAYMENT_FAILED_TOPIC, event, kv("orderId", event.orderId()));
        stockReservationService.release(event.orderId());
    }
}
