package com.shop.payment.messaging.config;

import com.shop.payment.filter.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
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

    @Bean
    RabbitTemplateCustomizer requestIdOnPublish() {
        return rabbitTemplate -> {
            rabbitTemplate.setObservationEnabled(true);
            rabbitTemplate.addBeforePublishPostProcessors(
                    message -> {
                        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
                        if (requestId != null) {
                            message.getMessageProperties().setHeader(RequestIdFilter.HEADER, requestId);
                        }
                        return message;
                    }
            );
        };
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setObservationEnabled(true);
        factory.setAfterReceivePostProcessors(message -> {
            Object requestId = message.getMessageProperties().getHeader(RequestIdFilter.HEADER);
            if (requestId != null) {
                MDC.put(RequestIdFilter.MDC_KEY, requestId.toString());
            } else {
                MDC.remove(RequestIdFilter.MDC_KEY);
            }

            return message;
        });
        return factory;
    }
}
