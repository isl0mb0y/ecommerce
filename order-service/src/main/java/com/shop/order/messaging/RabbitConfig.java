package com.shop.order.messaging;

import com.shop.order.order.PaymentFailed;
import com.shop.order.order.PaymentSucceeded;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "shop.events";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";
    public static final String PAYMENT_RESULT_QUEUE = "order-service.payment-result";

    @Bean
    TopicExchange shopEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue paymentResultQueue() {
        return new Queue(PAYMENT_RESULT_QUEUE);
    }

    @Bean
    Binding paymentSucceededBinding(Queue paymentResultQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(shopEventsExchange).with(PAYMENT_SUCCEEDED_KEY);
    }

    @Bean
    Binding paymentFailedBinding(Queue paymentResultQueue, TopicExchange shopEventsExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(shopEventsExchange).with(PAYMENT_FAILED_KEY);
    }

    // One queue carries two event types, so the routing key decides which record to read.
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
                    default -> throw new MessageConversionException("Unexpected routing key " + routingKey);
                };
            }
        });
        return converter;
    }
}
