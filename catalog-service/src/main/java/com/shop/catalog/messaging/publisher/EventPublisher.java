package com.shop.catalog.messaging.publisher;

import com.shop.catalog.filter.RequestIdFilter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, Long orderId, Object event) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, String.valueOf(orderId), event);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            record.headers().add(RequestIdFilter.HEADER, requestId.getBytes(StandardCharsets.UTF_8));
        }

        try {
            // Wait for the broker ack: a failed publish must fail the listener so the record is retried.
            kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish " + topic, e);
        }

        log.info("Published {}: {}", topic, event, kv("orderId", orderId));
    }
}
