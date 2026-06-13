package com.tutor.crmservice.message;

import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageThreadRepository threadRepository;
    @Mock MessageRepository messageRepository;
    @Mock CrmApplicationRepository applicationRepository;
    @Mock NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    MessageService service;

    @Test
    void sendTutorMessage_rejectsAnotherTutorsThread() {
        UUID tutorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        when(threadRepository.findByIdAndTutorUserId(threadId, tutorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.sendTutorMessage(tutorId, threadId, "Здравствуйте"));

        verify(messageRepository, never()).save(any());
        verify(notificationEventPublisher, never()).publishTutorMessage(any(), any(), any(), any());
    }

    @Test
    void sendTutorMessage_notifiesClientWithThreadContext() {
        UUID tutorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MessageThread thread = thread(threadId, applicationId, clientId, tutorId);
        CrmApplication application = application(applicationId, clientId, tutorId);
        application.setTutorName("Анна Иванова");

        when(threadRepository.findByIdAndTutorUserId(threadId, tutorId)).thenReturn(Optional.of(thread));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Message message = service.sendTutorMessage(tutorId, threadId, "Здравствуйте");

        assertEquals("Анна Иванова", message.getAuthorName());
        verify(notificationEventPublisher).publishTutorMessage(clientId, threadId, applicationId, "Анна Иванова");
    }

    @Test
    void sendClientMessage_notifiesTutorWithThreadContext() {
        UUID tutorId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MessageThread thread = thread(threadId, applicationId, clientId, tutorId);
        CrmApplication application = application(applicationId, clientId, tutorId);
        application.setStudentName("Мария");

        when(threadRepository.findByIdAndClientUserId(threadId, clientId)).thenReturn(Optional.of(thread));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Message message = service.sendClientMessage(clientId, threadId, "Спасибо");

        assertEquals("Вы", message.getAuthorName());
        verify(notificationEventPublisher).publishClientMessage(tutorId, threadId, applicationId, "Мария");
    }

    private MessageThread thread(UUID threadId, UUID applicationId, UUID clientId, UUID tutorId) {
        MessageThread thread = new MessageThread();
        thread.setId(threadId);
        thread.setApplicationId(applicationId);
        thread.setClientUserId(clientId);
        thread.setTutorUserId(tutorId);
        thread.setPreview("preview");
        return thread;
    }

    private CrmApplication application(UUID applicationId, UUID clientId, UUID tutorId) {
        CrmApplication application = new CrmApplication();
        application.setId(applicationId);
        application.setClientUserId(clientId);
        application.setTutorUserId(tutorId);
        application.setTutorName("Репетитор");
        application.setStudentName("Ученик");
        return application;
    }
}
