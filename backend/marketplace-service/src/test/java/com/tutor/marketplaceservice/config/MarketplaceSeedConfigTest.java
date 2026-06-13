package com.tutor.marketplaceservice.config;

import com.tutor.marketplaceservice.review.ReviewRepository;
import com.tutor.marketplaceservice.subject.SubjectRepository;
import com.tutor.marketplaceservice.tutor.TutorProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceSeedConfigTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TutorProfileRepository tutorProfileRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Test
    void existingDataExitsWithoutRuntimeSubjectBackfill() throws Exception {
        when(subjectRepository.count()).thenReturn(1L);

        CommandLineRunner runner = new MarketplaceSeedConfig()
                .marketplaceSeedRunner(subjectRepository, tutorProfileRepository, reviewRepository);

        runner.run();

        verify(subjectRepository, never()).findAll();
    }
}
