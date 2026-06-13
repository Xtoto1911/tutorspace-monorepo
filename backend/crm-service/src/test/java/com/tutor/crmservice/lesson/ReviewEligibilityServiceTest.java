package com.tutor.crmservice.lesson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewEligibilityServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private LessonRepository lessonRepository;

    @Test
    void startsAsSpringBeanWithRepositoryDependency() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(LessonRepository.class, () -> mock(LessonRepository.class));
            context.registerBean(ReviewEligibilityService.class);

            context.refresh();

            assertThat(context.getBean(ReviewEligibilityService.class)).isNotNull();
        }
    }

    @Test
    void allowsReviewWhenClientHasFinishedLessonWithTutor() {
        UUID clientUserId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        ReviewEligibilityService service = new ReviewEligibilityService(lessonRepository, clock);

        when(lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId))
                .thenReturn(List.of(lesson(clientUserId, tutorUserId, LocalDateTime.now(clock).minusMinutes(90), 60)));

        assertThat(service.hasFinishedLesson(clientUserId, tutorUserId)).isTrue();
    }

    @Test
    void rejectsFutureLessonsOtherClientsAndOtherTutors() {
        UUID clientUserId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        ReviewEligibilityService service = new ReviewEligibilityService(lessonRepository, clock);

        when(lessonRepository.findByClientUserIdOrderByStartsAtAsc(clientUserId))
                .thenReturn(List.of(
                        lesson(clientUserId, tutorUserId, LocalDateTime.now(clock).plusMinutes(30), 60),
                        lesson(UUID.randomUUID(), tutorUserId, LocalDateTime.now(clock).minusMinutes(90), 60),
                        lesson(clientUserId, UUID.randomUUID(), LocalDateTime.now(clock).minusMinutes(90), 60)
                ));

        assertThat(service.hasFinishedLesson(clientUserId, tutorUserId)).isFalse();
    }

    private Lesson lesson(UUID clientUserId, UUID tutorUserId, LocalDateTime startsAt, int durationMinutes) {
        Lesson lesson = new Lesson();
        lesson.setClientUserId(clientUserId);
        lesson.setTutorUserId(tutorUserId);
        lesson.setStartsAt(startsAt);
        lesson.setDurationMinutes(durationMinutes);
        lesson.setStatus("Запланировано");
        return lesson;
    }
}
