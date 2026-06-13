package com.tutor.crmservice.message;

import com.tutor.crmservice.kafka.NotificationEventPublisher;
import com.tutor.crmservice.request.CrmApplication;
import com.tutor.crmservice.request.CrmApplicationRepository;
import com.tutor.crmservice.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final CrmApplicationRepository applicationRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public MessageThread createThreadForApplication(CrmApplication application) {
        MessageThread thread = new MessageThread();
        thread.setApplicationId(application.getId());
        thread.setClientUserId(application.getClientUserId());
        thread.setTutorUserId(application.getTutorUserId());
        thread.setParticipantName(application.getTutorName());
        thread.setParticipantInitials(initials(application.getTutorName()));
        thread.setSubject(application.getSubjectName() + " · пробный урок");
        thread.setSubtitle("заявка отправлена");
        thread.setPreview(application.getComment());
        MessageThread saved = threadRepository.save(thread);
        addMessage(saved.getId(), "client", "Вы", "Здравствуйте! " + application.getComment());
        return saved;
    }

    @Transactional
    public void createAcceptNotification(CrmApplication application) {
        MessageThread thread = threadRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Диалог по заявке не найден."));
        thread.setSubtitle("заявка принята");
        thread.setPreview("Заявка принята. Можно согласовать первое занятие.");
        threadRepository.save(thread);
        addMessage(thread.getId(), "tutor", application.getTutorName(), "Здравствуйте! Я принял заявку и готов согласовать первое занятие.");
    }

    @Transactional
    public void createRejectNotification(CrmApplication application) {
        MessageThread thread = threadRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Диалог по заявке не найден."));
        thread.setSubtitle("заявка отклонена");
        thread.setPreview("Репетитор отклонил заявку.");
        threadRepository.save(thread);
        addMessage(thread.getId(), "tutor", application.getTutorName(), "К сожалению, я не могу принять эту заявку.");
    }

    @Transactional
    public Message sendTutorMessage(UUID tutorUserId, UUID threadId, String text) {
        MessageThread thread = threadRepository.findByIdAndTutorUserId(threadId, tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Диалог не найден."));
        String cleanedText = text.trim();
        thread.setPreview(cleanedText);
        threadRepository.save(thread);
        CrmApplication application = applicationRepository.findById(thread.getApplicationId()).orElse(null);
        String tutorName = application == null || application.getTutorName().isBlank() ? "Репетитор" : application.getTutorName();
        Message message = addMessage(thread.getId(), "tutor", tutorName, cleanedText);
        if (application != null) {
            notificationEventPublisher.publishTutorMessage(
                    thread.getClientUserId(),
                    thread.getId(),
                    thread.getApplicationId(),
                    tutorName
            );
        }
        return message;
    }

    @Transactional
    public Message sendClientMessage(UUID clientUserId, UUID threadId, String text) {
        MessageThread thread = threadRepository.findByIdAndClientUserId(threadId, clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Диалог не найден."));
        String cleanedText = text.trim();
        thread.setPreview(cleanedText);
        threadRepository.save(thread);
        Message message = addMessage(thread.getId(), "client", "Вы", cleanedText);
        applicationRepository.findById(thread.getApplicationId()).ifPresent(application ->
                notificationEventPublisher.publishClientMessage(
                        thread.getTutorUserId(),
                        thread.getId(),
                        thread.getApplicationId(),
                        application.getStudentName()
                )
        );
        return message;
    }

    private Message addMessage(UUID threadId, String authorRole, String authorName, String text) {
        Message message = new Message();
        message.setThreadId(threadId);
        message.setAuthorRole(authorRole);
        message.setAuthorName(authorName);
        message.setText(text);
        return messageRepository.save(message);
    }

    private String initials(String name) {
        return java.util.Arrays.stream(name.split(" "))
                .filter(part -> !part.isBlank())
                .map(part -> String.valueOf(part.charAt(0)))
                .limit(2)
                .reduce("", String::concat)
                .toUpperCase();
    }
}
