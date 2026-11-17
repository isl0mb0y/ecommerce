package com.shop.payment.messaging.config;

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
    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    public static final String STOCK_RESERVED_QUEUE = "payment-service.stock-reserved";
    public static final String STOCK_RESERVED_KEY = "stock.reserved";

    @Bean
    TopicExchange shopEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }


    @Bean
    Queue stockReservedQueue() {
        return new Queue(STOCK_RESERVED_QUEUE);
    }

    @Bean
    Binding stockReservedBinding(Queue stockReservedQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(stockReservedQueue).to(shopEventsExchange).with(STOCK_RESERVED_KEY);
    }

    // Deserialize into the listener's parameter type and ignore the sender's __TypeId__ header.
    @Bean
    MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
