package com.tutor.crmservice.kafka;

import com.tutor.crmservice.lesson.LessonRepository;
import com.tutor.crmservice.message.MessageThreadRepository;
import com.tutor.crmservice.request.CrmApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TutorNamePropagationService {

    private final CrmApplicationRepository crmApplicationRepository;
    private final LessonRepository lessonRepository;
    private final MessageThreadRepository messageThreadRepository;

    @Transactional
    public void propagateTutorName(UUID userId, String newName) {
        if (newName == null || newName.isBlank()) {
            log.warn("Skipping tutor name propagation for user {} because the name is empty", userId);
            return;
        }
        int applications = crmApplicationRepository.updateTutorNameByTutorUserId(userId, newName);
        int lessons = lessonRepository.updateTutorNameByTutorUserId(userId, newName);
        int threads = messageThreadRepository.updateParticipantNameByTutorUserId(userId, newName);
        log.info("Propagated tutor name '{}' to user {}: {} applications, {} lessons, {} threads",
                newName, userId, applications, lessons, threads);
    }
}
