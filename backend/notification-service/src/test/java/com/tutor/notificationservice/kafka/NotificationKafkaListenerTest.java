package com.tutor.notificationservice.kafka;

import com.tutor.notificationservice.notification.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = {"tutor-notifications", "client-notifications"})
@DirtiesContext
class NotificationKafkaListenerTest {

    @Autowired
    private KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate;

    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @TestConfiguration
    static class TestKafkaConfig {

        @Bean
        public ProducerFactory<String, NotificationEventEnvelope<NotificationPayload>> producerFactory(
                org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafka) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

            JsonSerializer<NotificationEventEnvelope<NotificationPayload>> serializer = new JsonSerializer<>();
            serializer.setAddTypeInfo(false);

            return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), serializer);
        }

        @Bean
        public KafkaTemplate<String, NotificationEventEnvelope<NotificationPayload>> kafkaTemplate(
                ProducerFactory<String, NotificationEventEnvelope<NotificationPayload>> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public ProducerFactory<String, String> stringProducerFactory(
                org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafka) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(props);
        }

        @Bean
        public KafkaTemplate<String, String> stringKafkaTemplate(ProducerFactory<String, String> stringProducerFactory) {
            return new KafkaTemplate<>(stringProducerFactory);
        }
    }

    @Test
    void shouldConsumeTutorNotificationEvent() {
        var userId = UUID.randomUUID();
        var payload = new NotificationPayload(
                userId,
                "Новая заявка",
                "Клиент отправил заявку",
                Map.of("tutorName", "Анна", "subjectName", "Математика")
        );
        var envelope = new NotificationEventEnvelope<>("REQUEST_CREATED", "marketplace-service", payload);

        kafkaTemplate.send("tutor-notifications", userId.toString(), envelope);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId.toString());
            assertEquals(1, notifications.size());
            assertEquals("Новая заявка", notifications.get(0).getTitle());
            assertEquals("REQUEST_CREATED", notifications.get(0).getEventType());
        });
    }

    @Test
    void shouldConsumeClientNotificationEvent() {
        var userId = UUID.randomUUID();
        var payload = new NotificationPayload(
                userId,
                "Заявка принята",
                "Репетитор принял заявку",
                Map.of("tutorName", "Анна", "subjectName", "Математика")
        );
        var envelope = new NotificationEventEnvelope<>("APPLICATION_ACCEPTED", "crm-service", payload);

        kafkaTemplate.send("client-notifications", userId.toString(), envelope);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId.toString());
            assertEquals(1, notifications.size());
            assertEquals("Заявка принята", notifications.get(0).getTitle());
            assertEquals("APPLICATION_ACCEPTED", notifications.get(0).getEventType());
        });
    }

    @Test
    void shouldConsumeEventWithForeignProducerTypeHeader() throws Exception {
        var userId = UUID.randomUUID();
        var payload = new NotificationPayload(
                userId,
                "Новая заявка",
                "Клиент отправил заявку",
                Map.of("tutorName", "Анна", "subjectName", "Математика")
        );
        var envelope = new NotificationEventEnvelope<>("REQUEST_CREATED", "marketplace-service", payload);
        var record = new ProducerRecord<>(
                "tutor-notifications",
                userId.toString(),
                objectMapper.writeValueAsString(envelope)
        );
        record.headers().add(new RecordHeader(
                "__TypeId__",
                "com.tutor.marketplaceservice.kafka.NotificationEventEnvelope".getBytes()
        ));

        stringKafkaTemplate.send(record);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId.toString());
            assertEquals(1, notifications.size());
            assertEquals("Новая заявка", notifications.get(0).getTitle());
            assertEquals("REQUEST_CREATED", notifications.get(0).getEventType());
        });
    }
}
