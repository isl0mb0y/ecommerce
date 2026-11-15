package com.shop.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerEventLogger {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventLogger.class);

    public CircuitBreakerEventLogger(CircuitBreakerRegistry registry) {
        registry.getAllCircuitBreakers().forEach(this::attach);
        registry.getEventPublisher().onEntryAdded(event -> attach(event.getAddedEntry()));
    }

    private void attach(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(e -> log.warn("CircuitBreaker '{}' state: {}",
                        e.getCircuitBreakerName(), e.getStateTransition()))
                .onError(e -> log.warn("CicuitBreaker '{}' recorded failure: {}",
                        e.getCircuitBreakerName(), e.getThrowable().toString()))
                .onCallNotPermitted(e -> log.info("CircuitBreaker '{}' rejected call (OPEN)",
                        e.getCircuitBreakerName()));
    }
}
