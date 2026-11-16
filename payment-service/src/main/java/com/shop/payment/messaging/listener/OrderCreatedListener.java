package com.shop.payment.messaging.listener;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.shop.payment.domain.Payment;
import com.shop.payment.domain.PaymentStatus;
import com.shop.payment.messaging.config.RabbitConfig;
import com.shop.payment.messaging.event.OrderCreated;
import com.shop.payment.messaging.event.PaymentFailed;
import com.shop.payment.messaging.event.PaymentSucceeded;
import com.shop.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderCreatedListener(PaymentRepository paymentRepository, RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreated event) throws InterruptedException {
        log.info("Consumed {}: {}", RabbitConfig.ORDER_CREATED_KEY, event);
        // A redelivered order reuses its payment, so it is never charged twice.
        Payment payment = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (payment == null) {
            payment = process(event);
        }
        publishResult(payment, event);
    }

    private Payment process(OrderCreated event) throws InterruptedException {
        Thread.sleep(2000);
        PaymentStatus status = ThreadLocalRandom.current().nextInt(100) < 80
                ? PaymentStatus.SUCCESS
                : PaymentStatus.FAILED;
        return paymentRepository.save(new Payment(event.orderId(), event.totalInCents(), status));
    }

    private void publishResult(Payment payment, OrderCreated event) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            List<PaymentSucceeded.Item> items = event.items().stream()
                    .map(item -> new PaymentSucceeded.Item(item.productId(), item.quantity()))
                    .toList();
            String eventId = "payment-succeeded-" + payment.getId();
            PaymentSucceeded result = new PaymentSucceeded(eventId, event.orderId(), payment.getId(), items);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PAYMENT_SUCCEEDED_KEY, result);
            log.info("Published {}: {}", RabbitConfig.PAYMENT_SUCCEEDED_KEY, result);
        } else {
            PaymentFailed result = new PaymentFailed(event.orderId(), "Payment declined");
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PAYMENT_FAILED_KEY, result);
            log.info("Published {}: {}", RabbitConfig.PAYMENT_FAILED_KEY, result);
        }
    }
}
