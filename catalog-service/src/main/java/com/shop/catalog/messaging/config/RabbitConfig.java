package com.shop.catalog.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "shop.events";

    public static final String PAYMENT_SUCCEEDED_QUEUE = "catalog-service.payment-succeeded";
    public static final String ORDER_CREATED_QUEUE = "catalog-service.order-created";
    public static final String PAYMENT_FAILED_QUEUE = "catalog-service.payment-failed";

    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String STOCK_RESERVED_KEY = "stock.reserved";
    public static final String STOCK_REJECTED_KEY = "stock.rejected";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    @Bean
    TopicExchange shopEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue paymentSucceededQueue() {
        return new Queue(PAYMENT_SUCCEEDED_QUEUE);
    }

    @Bean
    Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE);
    }

    @Bean
    Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE);
    }

    @Bean
    Binding paymentSucceededBinding(Queue paymentSucceededQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(paymentSucceededQueue).to(shopEventsExchange).with(PAYMENT_SUCCEEDED_KEY);
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange shopEventExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(shopEventExchange).with(ORDER_CREATED_KEY);
    }

    @Bean
    Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange shopEventExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(shopEventExchange).with(PAYMENT_FAILED_KEY);
    }

    // Deserialize into the listener's parameter type and ignore the sender's __TypeId__ header.
    @Bean
    MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
