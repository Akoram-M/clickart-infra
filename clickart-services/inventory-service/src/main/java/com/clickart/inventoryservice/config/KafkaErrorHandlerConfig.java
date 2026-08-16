package com.clickart.inventoryservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        
        // 1. The Recoverer: Where does the message go when retries are exhausted?
        // Default behavior appends ".DLQ" to the original topic name.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) -> new TopicPartition(record.topic() + ".DLQ", record.partition())
        );

        // 2. The BackOff Strategy: Wait 1000ms between attempts, maximum of 3 attempts.
        FixedBackOff backOff = new FixedBackOff(1000L, 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Optional: Do not retry on specific fatal exceptions (like JSON parsing errors)
        errorHandler.addNotRetryableExceptions(MessageConversionException.class);

        return errorHandler;
    }
}