package com.shop.order.messaging.listener;

import com.shop.order.messaging.config.RabbitConfig;
import com.shop.order.messaging.event.PaymentFailed;
import com.shop.order.messaging.event.PaymentSucceeded;
import com.shop.order.messaging.event.StockRejected;
import com.shop.order.service.OrderService;
import com.shop.order.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitConfig.ORDER_EVENTS_QUEUE)
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final OrderService orderService;

    public OrderEventsListener(OrderService orderService) {
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

    @RabbitHandler
    public void onStockRejected(StockRejected event) {
        log.info("Consumed {}: {}", RabbitConfig.STOCK_REJECTED_KEY, event);
        orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED);
    }
}
