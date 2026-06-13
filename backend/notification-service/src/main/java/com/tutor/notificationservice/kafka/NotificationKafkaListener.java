package com.tutor.notificationservice.kafka;

import com.tutor.notificationservice.notification.NotificationIngestDto;
import com.tutor.notificationservice.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = {"tutor-notifications", "client-notifications"})
    public void onNotificationEvent(NotificationEventEnvelope<NotificationPayload> envelope) {
        log.info("Received notification event: {} from producer: {}", envelope.eventType(), envelope.producer());

        var payload = envelope.payload();
        var dto = new NotificationIngestDto(
                payload.userId().toString(),
                envelope.eventType(),
                payload.title(),
                payload.message(),
                payload.metadata()
        );

        notificationService.ingest(dto);
    }
}
