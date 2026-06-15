package com.tutor.crmservice.kafka;

import com.tutor.crmservice.lesson.LessonRepository;
import com.tutor.crmservice.message.MessageThreadRepository;
import com.tutor.crmservice.request.CrmApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorNamePropagationServiceTest {

    @Mock private CrmApplicationRepository crmApplicationRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private MessageThreadRepository messageThreadRepository;

    @InjectMocks
    private TutorNamePropagationService service;

    @Test
    void propagateTutorNameUpdatesAllThreeTablesForUser() {
        UUID userId = UUID.randomUUID();
        when(crmApplicationRepository.updateTutorNameByTutorUserId(userId, "Пётр")).thenReturn(3);
        when(lessonRepository.updateTutorNameByTutorUserId(userId, "Пётр")).thenReturn(2);
        when(messageThreadRepository.updateParticipantNameByTutorUserId(userId, "Пётр")).thenReturn(1);

        service.propagateTutorName(userId, "Пётр");

        verify(crmApplicationRepository).updateTutorNameByTutorUserId(userId, "Пётр");
        verify(lessonRepository).updateTutorNameByTutorUserId(userId, "Пётр");
        verify(messageThreadRepository).updateParticipantNameByTutorUserId(userId, "Пётр");
    }

    @Test
    void propagateTutorNameSkipsRepositoriesWhenNameIsNull() {
        UUID userId = UUID.randomUUID();
        service.propagateTutorName(userId, null);

        verify(crmApplicationRepository, never()).updateTutorNameByTutorUserId(userId, null);
        verify(lessonRepository, never()).updateTutorNameByTutorUserId(userId, null);
        verify(messageThreadRepository, never()).updateParticipantNameByTutorUserId(userId, null);
    }
}
