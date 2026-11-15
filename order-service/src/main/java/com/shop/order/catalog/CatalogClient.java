package com.shop.order.catalog;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class CatalogClient {

    private final RestClient restClient;
    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    public CatalogClient(
            RestClient.Builder builder,
            @Value("${catalog.base-url}") String baseUrl,
            @Value("${catalog.connect-timeout-ms:500}") int connectionTimeoutMs,
            @Value("${catalog.read-timeout-ms:2000}") int readTimeoutMs
            ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectionTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog")
    public Optional<ProductResponse> findProduct(Long id) {
        log.info("Calling catalog-service for product {}", id);
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/products/{id}", id)
                    .retrieve()
                    .body(ProductResponse.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
