package com.shop.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerEventLogger {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventLogger.class);

    public CircuitBreakerEventLogger(CircuitBreakerRegistry registry) {
        registry.circuitBreaker("catalog").getEventPublisher()
                .onStateTransition(e -> log.warn("CircuitBreaker '{}' state: {}",
                        e.getCircuitBreakerName(), e.getStateTransition()))
                .onCallNotPermitted(e -> log.info("CircuitBreaker '{}' rejected call (OPEN)",
                        e.getCircuitBreakerName()));
    }
}
