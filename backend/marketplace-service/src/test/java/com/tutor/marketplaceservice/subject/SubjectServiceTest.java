package com.tutor.marketplaceservice.subject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    void createSubjectPersistsAndExposesDescription() {
        when(subjectRepository.existsBySlug("matematika")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectDto dto = subjectService.createSubject(new UpsertSubjectRequest(
                "Математика",
                "matematika",
                "Школьная математика, подготовка к ОГЭ, ЕГЭ и олимпиадам."
        ));

        assertThat(dto.description())
                .isEqualTo("Школьная математика, подготовка к ОГЭ, ЕГЭ и олимпиадам.");
    }
}
