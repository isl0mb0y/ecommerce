package com.shop.order.config;

import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryEventLogger {
    private static final Logger log = LoggerFactory.getLogger(RetryEventLogger.class);

    public RetryEventLogger(RetryRegistry retryRegistry) {
        retryRegistry.retry("catalog").getEventPublisher()
                .onRetry(e -> log.warn("Retry '{}' attempt #{} after: {}",
                        e.getName(), e.getNumberOfRetryAttempts(),e.getLastThrowable().getMessage()))
                .onError(e -> log.error("Retry '{}' exhausted after {} attempts: {}",
                        e.getName(), e.getNumberOfRetryAttempts(), e.getLastThrowable().getMessage()))
                .onSuccess(e -> log.info("Retry '{}' succeeded after {} retries",
                        e.getName(), e.getNumberOfRetryAttempts()))
                .onIgnoredError(e -> log.info("Retry '{}' ignored error (no retry): {}",
                        e.getName(), e.getLastThrowable().getMessage()));
    }
}
