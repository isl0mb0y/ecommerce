package com.shop.catalog.repository;

import com.shop.catalog.domain.ReservationStatus;
import com.shop.catalog.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
}
