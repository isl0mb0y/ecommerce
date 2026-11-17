package com.shop.order.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    // Skip traces for @Scheduled runs: the outbox poller fires every second and is mostly empty.
    @Bean
    ObservationPredicate skipScheduledTasks() {
        return (name, context) -> !"tasks.scheduled.execution".equals(name);
    }
}
