package com.shop.order.messaging.config;

import com.shop.order.filter.RequestIdFilter;
import com.shop.order.messaging.event.PaymentFailed;
import com.shop.order.messaging.event.PaymentSucceeded;
import com.shop.order.messaging.event.StockRejected;
import org.slf4j.MDC;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "shop.events";

    public static final String ORDER_EVENTS_QUEUE = "order-service.events";

    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";
    public static final String STOCK_REJECTED_KEY = "stock.rejected";

    @Bean
    TopicExchange shopEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue orderEventsQueue() {
        return new Queue(ORDER_EVENTS_QUEUE);
    }

    @Bean
    Binding paymentSucceededBinding(Queue orderEventsQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(orderEventsQueue).to(shopEventsExchange).with(PAYMENT_SUCCEEDED_KEY);
    }

    @Bean
    Binding paymentFailedBinding(Queue paymentResultQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(shopEventsExchange).with(PAYMENT_FAILED_KEY);
    }

    @Bean
    Binding stockRejectedBinding(Queue paymentResultQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(shopEventsExchange).with(STOCK_REJECTED_KEY);
    }

    // One queue carries several event types, so the routing key decides which record to read.
    @Bean
    MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(new ClassMapper() {
            @Override
            public void fromClass(Class<?> clazz, MessageProperties properties) {
                // No __TypeId__ header: consumers pick their own type.
            }

            @Override
            public Class<?> toClass(MessageProperties properties) {
                String routingKey = properties.getReceivedRoutingKey();
                return switch (routingKey) {
                    case PAYMENT_SUCCEEDED_KEY -> PaymentSucceeded.class;
                    case PAYMENT_FAILED_KEY -> PaymentFailed.class;
                    case STOCK_REJECTED_KEY ->  StockRejected.class;
                    default -> throw new MessageConversionException("Unexpected routing key " + routingKey);
                };
            }
        });
        return converter;
    }

    @Bean
    RabbitTemplateCustomizer requestIdOnPublish() {
        return rabbitTemplate -> rabbitTemplate.addBeforePublishPostProcessors(
                message -> {
                    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
                    if (requestId != null) {
                        message.getMessageProperties().setHeader(RequestIdFilter.HEADER, requestId);
                    }

                    return message;
                }
        );
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
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
