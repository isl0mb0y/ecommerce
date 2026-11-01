package com.shop.catalog.product;

import com.shop.catalog.messaging.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentSucceededListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededListener.class);

    private final ProductRepository productRepository;

    public PaymentSucceededListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_SUCCEEDED_QUEUE)
    public void onPaymentSucceeded(PaymentSucceeded event) {
        log.info("Consumed {}: {}", RabbitConfig.PAYMENT_SUCCEEDED_KEY, event);
        for (PaymentSucceeded.Item item : event.items()) {
            int updated = productRepository.decrementStock(item.productId(), item.quantity());
            if (updated == 0) {
                log.warn("Stock not decremented for product {} (order {}): quantity {} would make it negative or product is missing",
                        item.productId(), event.orderId(), item.quantity());
            }
        }
    }
}
