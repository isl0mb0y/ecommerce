package com.shop.order.messaging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order.created";

    // The publishing service owns the topic and creates it on startup
    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC).partitions(3).replicas(1).build();
    }
}
