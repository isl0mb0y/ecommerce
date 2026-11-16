package com.shop.catalog.messaging.listener;

import com.shop.catalog.messaging.config.RabbitConfig;
import com.shop.catalog.messaging.event.PaymentSucceeded;
import com.shop.catalog.messaging.event.ProcessedEventRepository;
import com.shop.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentSucceededListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededListener.class);

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;

    public PaymentSucceededListener(ProductRepository productRepository, ProcessedEventRepository processedEventRepository) {
        this.productRepository = productRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_SUCCEEDED_QUEUE)
    @Transactional
    public void onPaymentSucceeded(PaymentSucceeded event) {
        log.info("Consumed {}: {}", RabbitConfig.PAYMENT_SUCCEEDED_KEY, event);
        if (processedEventRepository.markProcessed(event.eventId()) == 0) {
            log.info("Duplicate event {} skipped", event.eventId());
            return;
        }
        for (PaymentSucceeded.Item item : event.items()) {
            int updated = productRepository.decrementStock(item.productId(), item.quantity());
            if (updated == 0) {
                log.warn("Stock not decremented for product {} (order {}): quantity {} would make it negative or product is missing",
                        item.productId(), event.orderId(), item.quantity());
            }
        }
    }
}
