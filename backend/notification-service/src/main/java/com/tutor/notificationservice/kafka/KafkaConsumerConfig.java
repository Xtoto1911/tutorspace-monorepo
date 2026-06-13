package com.tutor.notificationservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, NotificationEventEnvelope<NotificationPayload>> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:${spring.application.name}}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset:earliest}") String autoOffsetReset,
            ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        JavaType envelopeType = objectMapper.getTypeFactory().constructParametricType(
                NotificationEventEnvelope.class,
                NotificationPayload.class
        );
        JsonDeserializer<NotificationEventEnvelope<NotificationPayload>> jsonDeserializer =
                new JsonDeserializer<>(envelopeType, objectMapper, false);
        jsonDeserializer.addTrustedPackages("com.tutor.*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEventEnvelope<NotificationPayload>>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, NotificationEventEnvelope<NotificationPayload>> consumerFactory,
            DefaultErrorHandler notificationKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEventEnvelope<NotificationPayload>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(notificationKafkaErrorHandler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler notificationKafkaErrorHandler() {
        return new DefaultErrorHandler((record, exception) -> log.error(
                "Failed to consume notification event from topic {} partition {} offset {} key {}: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                exception.getMessage(),
                exception
        ), new FixedBackOff(0L, 0L));
    }
}
