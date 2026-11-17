package com.shop.catalog.messaging.listener;

import com.shop.catalog.messaging.config.RabbitConfig;
import com.shop.catalog.messaging.event.PaymentFailed;
import com.shop.catalog.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedListener.class);

    private final StockReservationService stockReservationService;

    public PaymentFailedListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailed event) {
        log.info("Consumed {}: {}", RabbitConfig.PAYMENT_FAILED_KEY, event);
        stockReservationService.release(event.orderId());
    }
}
