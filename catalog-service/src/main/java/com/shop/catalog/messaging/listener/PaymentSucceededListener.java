package com.shop.catalog.messaging.listener;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.shop.catalog.messaging.config.RabbitConfig;
import com.shop.catalog.messaging.event.PaymentSucceeded;
import com.shop.catalog.repository.ProcessedEventRepository;
import com.shop.catalog.repository.ProductRepository;
import com.shop.catalog.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentSucceededListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededListener.class);

    private final StockReservationService stockReservationService;

    public PaymentSucceededListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_SUCCEEDED_QUEUE)
    @Transactional
    public void onPaymentSucceeded(PaymentSucceeded event) {
        log.info("Consumed {}: {}", RabbitConfig.PAYMENT_SUCCEEDED_KEY, event, kv("orderId", event.orderId()));
        stockReservationService.confirm(event.orderId());
    }
}
