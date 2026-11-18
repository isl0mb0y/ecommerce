package com.shop.order.messaging.publisher;

import com.shop.order.domain.OutboxEvent;
import com.shop.order.domain.OutboxStatus;
import com.shop.order.filter.RequestIdFilter;
import com.shop.order.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        for (OutboxEvent event: outboxEventRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING)) {
            try {

                if (event.getRequestId() != null) {
                    MDC.put(RequestIdFilter.MDC_KEY, event.getRequestId());
                }

                // routingKey now holds the Kafka topic name; messageKey is the orderId.
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(
                                event.getRoutingKey(),
                                event.getMessageKey(),
                                event.getPayload());

                if (event.getRequestId() != null) {
                    record.headers().add(RequestIdFilter.HEADER,
                            event.getRequestId().getBytes(StandardCharsets.UTF_8));
                }
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                event.markPublished();
                logger.info("Published outbox event {} {}", kv("outboxId", event.getId()), kv("routingKey", event.getRoutingKey()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException e) {
                logger.warn("Outbox publish failed, will retry: {}", e.getMessage());
                return;
            } finally {
                MDC.remove(RequestIdFilter.MDC_KEY);
            }
        }
    }
}
