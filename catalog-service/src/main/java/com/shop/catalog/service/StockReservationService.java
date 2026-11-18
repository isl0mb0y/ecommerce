package com.shop.catalog.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.shop.catalog.domain.ReservationStatus;
import com.shop.catalog.domain.StockReservation;
import com.shop.catalog.messaging.config.KafkaConfig;
import com.shop.catalog.messaging.event.OrderCreated;
import com.shop.catalog.messaging.event.StockRejected;
import com.shop.catalog.messaging.event.StockReserved;
import com.shop.catalog.messaging.publisher.EventPublisher;
import com.shop.catalog.repository.ProcessedEventRepository;
import com.shop.catalog.repository.ProductRepository;
import com.shop.catalog.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final EventPublisher eventPublisher;

    public StockReservationService(ProductRepository productRepository, StockReservationRepository reservationRepository, ProcessedEventRepository processedEventRepository, EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void reserve(OrderCreated event) {
        Long orderId = event.orderId();
        if (processedEventRepository.markProcessed("order-created-" + orderId) == 0) {
            log.info("Order {} already handled, skipping", orderId, kv("orderId", orderId));
            return;
        }

        List<OrderCreated.Item> reserved = new ArrayList<>();
        for (OrderCreated.Item item: event.items()) {
            if (productRepository.decrementStock(item.productId(), item.quantity()) == 0) {
                reserved.forEach(r -> productRepository.incrementStock(r.productId(), r.quantity()));
                StockRejected rejected = new StockRejected("stock-rejected-" + orderId, orderId, "Insufficient stock for product " + item.productId());
                eventPublisher.publish(KafkaConfig.STOCK_REJECTED_TOPIC, orderId, rejected);
                return;
            }
            reserved.add(item);
        }

        reserved.forEach(i -> reservationRepository.save(
                new StockReservation(orderId, i.productId(), i.quantity())));
        StockReserved result = new StockReserved("stock-reserved-" + orderId, orderId, event.totalInCents());
        eventPublisher.publish(KafkaConfig.STOCK_RESERVED_TOPIC, orderId, result);

    }

    @Transactional
    public void confirm(Long orderId) {
        reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED)
                .forEach(StockReservation::confirm);
        log.info("Confirmed reservations for order {}", orderId, kv("orderId", orderId));
    }

    @Transactional
    public void release(Long orderId) {
        List<StockReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (StockReservation reservation: reservations) {
            productRepository.incrementStock(reservation.getProductId(), reservation.getQuantity());
            reservation.release();
        }

        log.info("Released {} reservation(s) for order {}", reservations.size(), orderId, kv("orderId", orderId));
    }
}
