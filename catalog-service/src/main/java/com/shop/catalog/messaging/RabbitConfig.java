package com.shop.catalog.messaging;

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
    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";

    @Bean
    TopicExchange shopEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue paymentSucceededQueue() {
        return new Queue(PAYMENT_SUCCEEDED_QUEUE);
    }

    @Bean
    Binding paymentSucceededBinding(Queue paymentSucceededQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(paymentSucceededQueue).to(shopEventsExchange).with(PAYMENT_SUCCEEDED_KEY);
    }

    // Deserialize into the listener's parameter type and ignore the sender's __TypeId__ header.
    @Bean
    MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
