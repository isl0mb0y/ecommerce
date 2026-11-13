package com.shop.order.order;

import com.shop.order.messaging.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitConfig.PAYMENT_RESULT_QUEUE)
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final OrderService orderService;

    public PaymentResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceeded event) {
        log.info("Consumed {}: {}", RabbitConfig.PAYMENT_SUCCEEDED_KEY, event);
        orderService.updateStatus(event.orderId(), OrderStatus.PAID);
    }

    @RabbitHandler
    public void onPaymentFailed(PaymentFailed event) {
        log.info("Consumed {}: {}", RabbitConfig.PAYMENT_FAILED_KEY, event);
        orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED);
    }
}
