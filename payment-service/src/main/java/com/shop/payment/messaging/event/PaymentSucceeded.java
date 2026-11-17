package com.shop.payment.messaging.event;


public record PaymentSucceeded(String eventId, Long orderId, Long paymentId) {

}
