package com.tutor.crmservice.lesson;

import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.service.RequestLessonRescheduleDto;
import com.tutor.crmservice.shared.ConflictException;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonRescheduleService {

    private final LessonRepository lessonRepository;
    private final LessonRescheduleRequestRepository rescheduleRequestRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public LessonRescheduleRequest createClientRequest(UUID clientUserId, UUID lessonId, RequestLessonRescheduleDto input) {
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден."));

        if (!lesson.getClientUserId().equals(clientUserId)) {
            throw new ResourceNotFoundException("Урок не найден.");
        }

        if (rescheduleRequestRepository.existsByLessonIdAndStatus(lessonId, LessonRescheduleStatus.PENDING)) {
            throw new ConflictException("Запрос на перенос уже ожидает ответа.");
        }

        String message = normalizeMessage(input);

        LessonRescheduleRequest request = new LessonRescheduleRequest();
        request.setLessonId(lesson.getId());
        request.setApplicationId(lesson.getApplicationId());
        request.setClientUserId(lesson.getClientUserId());
        request.setTutorUserId(lesson.getTutorUserId());
        request.setLessonTitle(lesson.getTitle());
        request.setLessonStartsAt(lesson.getStartsAt());
        request.setMessage(message);
        request.setProposedStartAt(input.proposedStartAt());
        request.setStatus(LessonRescheduleStatus.PENDING);
        LessonRescheduleRequest saved = rescheduleRequestRepository.save(request);
        notificationEventPublisher.publishRescheduleRequested(
                lesson.getTutorUserId(),
                lesson.getApplicationId(),
                lesson.getId(),
                lesson.getTitle(),
                message
        );
        return saved;
    }

    private String normalizeMessage(RequestLessonRescheduleDto input) {
        if (input == null || input.message() == null) {
            throw new IllegalArgumentException("Сообщение для переноса обязательно.");
        }

        String normalizedMessage = input.message().trim();
        if (normalizedMessage.isEmpty()) {
            throw new IllegalArgumentException("Сообщение для переноса обязательно.");
        }
        if (normalizedMessage.length() > 1000) {
            throw new IllegalArgumentException("Сообщение для переноса не должно превышать 1000 символов.");
        }
        return normalizedMessage;
    }
}
