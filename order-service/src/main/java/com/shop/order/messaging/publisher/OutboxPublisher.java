package com.shop.order.messaging.publisher;

import com.shop.order.domain.OutboxEvent;
import com.shop.order.domain.OutboxStatus;
import com.shop.order.filter.RequestIdFilter;
import com.shop.order.messaging.config.RabbitConfig;
import com.shop.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        for (OutboxEvent event: outboxEventRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING)) {
            try {
                if (event.getRequestId() != null) {
                    MDC.put(RequestIdFilter.MDC_KEY, event.getRequestId());
                }
                Message message = MessageBuilder
                        .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();
                rabbitTemplate.send(RabbitConfig.EXCHANGE, event.getRoutingKey(), message);
                event.markPublished();
                logger.info("Published outbox event {} {}", kv("outboxId", event.getId()), kv("routingKey", event.getRoutingKey()));
            } catch (AmqpException e) {
                logger.warn("Outbox publish failed, will retry: {}", e.getMessage());
                return;
            } finally {
                MDC.remove(RequestIdFilter.MDC_KEY);
            }
        }
    }
}
