package com.shop.payment.messaging.listener;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.concurrent.ThreadLocalRandom;

import com.shop.payment.domain.Payment;
import com.shop.payment.domain.PaymentStatus;
import com.shop.payment.messaging.config.KafkaConfig;
import com.shop.payment.messaging.event.PaymentFailed;
import com.shop.payment.messaging.event.PaymentSucceeded;
import com.shop.payment.messaging.event.StockReserved;
import com.shop.payment.messaging.publisher.EventPublisher;
import com.shop.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StockReservedListener {

    private static final Logger log = LoggerFactory.getLogger(StockReservedListener.class);

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    public StockReservedListener(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = KafkaConfig.STOCK_RESERVED_TOPIC)
    public void onStockReserved(StockReserved event) throws InterruptedException {
        log.info("Consumed {}: {}", KafkaConfig.STOCK_RESERVED_TOPIC, event, kv("orderId", event.orderId()));
        // A redelivered event reuses its payment, so the order is never charged twice.
        Payment payment = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (payment == null) {
            payment = process(event);
        }
        publishResult(payment, event);
    }

    private Payment process(StockReserved event) throws InterruptedException {
        Thread.sleep(2000);
        PaymentStatus status = ThreadLocalRandom.current().nextInt(100) < 80
                ? PaymentStatus.SUCCESS
                : PaymentStatus.FAILED;
        return paymentRepository.save(new Payment(event.orderId(), event.totalInCents(), status));
    }

    private void publishResult(Payment payment, StockReserved event) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            String eventId = "payment-succeeded-" + payment.getId();
            PaymentSucceeded result = new PaymentSucceeded(eventId, event.orderId(), payment.getId());
            eventPublisher.publish(KafkaConfig.PAYMENT_SUCCEEDED_TOPIC, event.orderId(), result);
        } else {
            PaymentFailed result = new PaymentFailed(event.orderId(), "Payment declined");
            eventPublisher.publish(KafkaConfig.PAYMENT_FAILED_TOPIC, event.orderId(), result);
        }
    }
}
