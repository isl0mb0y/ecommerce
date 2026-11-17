package com.shop.order.config;

import com.shop.order.domain.OutboxStatus;
import com.shop.order.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
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

    // Exposed to Prometheus as outbox_pending; evaluated on every scrape.
    @Bean
    MeterBinder outboxPendingGauge(OutboxEventRepository outboxEventRepository) {
        return registry -> Gauge.builder("outbox.pending", outboxEventRepository,
                        repository -> repository.countByStatus(OutboxStatus.PENDING))
                .description("Outbox events waiting to be published")
                .register(registry);
    }
}
