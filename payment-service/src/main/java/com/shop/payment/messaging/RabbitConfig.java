package com.shop.payment.messaging;

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
    public static final String ORDER_CREATED_QUEUE = "payment-service.order-created";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    @Bean
    TopicExchange shopEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE);
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(shopEventsExchange).with(ORDER_CREATED_KEY);
    }

    // Deserialize into the listener's parameter type and ignore the sender's __TypeId__ header.
    @Bean
    MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
