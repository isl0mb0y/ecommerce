package com.shop.catalog.messaging.config;

import com.shop.catalog.filter.RequestIdFilter;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.nio.charset.StandardCharsets;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order.created";

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
