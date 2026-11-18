package com.shop.order.messaging.config;

import com.shop.order.filter.RequestIdFilter;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.nio.charset.StandardCharsets;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order.created";
    public static final String PAYMENT_SUCCEEDED_TOPIC = "payment.succeeded";
    public static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    public static final String STOCK_REJECTED_TOPIC = "stock.rejected";

    // The publishing service owns the topic and creates it on startup
    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC).partitions(3).replicas(1).build();
    }

    // Converts the JSON string payload into the listener's parameter type.
    @Bean
    RecordMessageConverter recordMessageConverter() {
        return new StringJsonMessageConverter();
    }

    // Copies X-Request-Id from the record header into the MDC of the listener thread.
    @Bean
    RecordInterceptor<Object, Object> requestIdInterceptor() {
        return (record, consumer) -> {
            Header header = record.headers().lastHeader(RequestIdFilter.HEADER);
            if (header != null) {
                MDC.put(RequestIdFilter.MDC_KEY, new String(header.value(), StandardCharsets.UTF_8));
            } else {
                MDC.remove(RequestIdFilter.MDC_KEY);
            }

            return record;
        };
    }
}
