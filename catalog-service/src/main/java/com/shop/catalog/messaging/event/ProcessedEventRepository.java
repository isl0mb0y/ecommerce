package com.shop.catalog.messaging.event;

import com.shop.catalog.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    @Modifying
    @Query(value = "insert into processed_events (event_id, processed_at) values (:eventId, now()) on conflict do nothing",
    nativeQuery = true)
    int markProcessed(@Param("eventId") String eventId);
}
