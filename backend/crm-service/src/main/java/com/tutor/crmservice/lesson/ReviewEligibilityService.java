package com.tutor.crmservice.lesson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReviewEligibilityService {

    private final LessonRepository lessonRepository;
    private final Clock clock;

    @Autowired
    public ReviewEligibilityService(LessonRepository lessonRepository) {
        this(lessonRepository, Clock.systemDefaultZone());
    }

    ReviewEligibilityService(LessonRepository lessonRepository, Clock clock) {
        this.lessonRepository = lessonRepository;
        this.clock = clock;
    }

    public boolean hasFinishedLesson(UUID clientUserId, UUID tutorUserId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId).stream()
                .anyMatch(lesson -> clientUserId.equals(lesson.getClientUserId())
                        && tutorUserId.equals(lesson.getTutorUserId())
                        && lesson.getStartsAt() != null
                        && lesson.getDurationMinutes() != null
                        && !lesson.getStartsAt().plusMinutes(lesson.getDurationMinutes()).isAfter(now));
    }

    public ReviewEligibilityDto getEligibility(UUID clientUserId, UUID tutorUserId) {
        return hasFinishedLesson(clientUserId, tutorUserId)
                ? ReviewEligibilityDto.allowed()
                : ReviewEligibilityDto.denied();
    }
}
