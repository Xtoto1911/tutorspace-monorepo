package com.tutor.marketplaceservice.tutor;

import com.tutor.marketplaceservice.subject.Subject;
import com.tutor.marketplaceservice.subject.SubjectRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TutorProfilePersistenceTest {

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void reloadsExtendedProfileFieldsAndMapsThemToDto() {
        Subject subject = new Subject();
        subject.setName("Физика");
        subject.setSlug("fizika");
        subject.setDescription("Школьная физика и подготовка к экзаменам.");
        Subject savedSubject = subjectRepository.saveAndFlush(subject);

        TutorProfile profile = new TutorProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setSlug("aleksandr-fizika");
        profile.setFullName("Александр Орлов");
        profile.setDescription("Объясняю физику через эксперименты и задачи.");
        profile.setCity("Москва");
        profile.setEducation(List.of("МФТИ, факультет общей и прикладной физики"));
        profile.setHighlights(List.of("Средний результат ЕГЭ учеников — 88 баллов"));
        profile.setTags(List.of("ЕГЭ", "Олимпиады"));
        profile.setResponseTimeMinutes(25);
        profile.setExperienceYears(9);
        profile.setPricePerHour(new BigDecimal("2200.00"));
        profile.setPhotoUrl("https://example.com/aleksandr.jpg");
        profile.setRating(4.9);
        profile.setReviewsCount(42);
        profile.setStatus(TutorProfileStatus.ACTIVE);
        profile.setSubjects(new LinkedHashSet<>(List.of(savedSubject)));
        UUID profileId = tutorProfileRepository.saveAndFlush(profile).getId();

        entityManager.clear();

        TutorProfileDto dto = TutorProfileDto.from(tutorProfileRepository.findById(profileId).orElseThrow());

        assertThat(dto.city()).isEqualTo("Москва");
        assertThat(dto.education()).containsExactly("МФТИ, факультет общей и прикладной физики");
        assertThat(dto.highlights()).containsExactly("Средний результат ЕГЭ учеников — 88 баллов");
        assertThat(dto.tags()).containsExactly("ЕГЭ", "Олимпиады");
        assertThat(dto.responseTimeMinutes()).isEqualTo(25);
        assertThat(dto.subjects()).singleElement().satisfies(subjectDto -> {
            assertThat(subjectDto.slug()).isEqualTo("fizika");
            assertThat(subjectDto.description()).isEqualTo("Школьная физика и подготовка к экзаменам.");
        });
    }
}
