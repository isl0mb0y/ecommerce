package com.shop.order.repository;

import com.shop.order.domain.OutboxEvent;
import com.shop.order.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByIdAsc(OutboxStatus status);
    long countByStatus(OutboxStatus status);
}
