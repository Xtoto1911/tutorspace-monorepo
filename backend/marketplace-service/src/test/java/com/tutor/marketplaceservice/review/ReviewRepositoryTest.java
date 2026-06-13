package com.tutor.marketplaceservice.review;

import com.tutor.marketplaceservice.tutor.TutorProfile;
import com.tutor.marketplaceservice.tutor.TutorProfileRepository;
import com.tutor.marketplaceservice.tutor.TutorProfileStatus;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Test
    void findByStatusLoadsTutorProfileForModerationDtoMapping() {
        TutorProfile tutorProfile = new TutorProfile();
        tutorProfile.setUserId(UUID.randomUUID());
        tutorProfile.setFullName("Анна Иванова");
        tutorProfile.setSlug("anna-math");
        tutorProfile.setDescription("Математика без тревоги");
        tutorProfile.setExperienceYears(6);
        tutorProfile.setPricePerHour(new BigDecimal("1600.00"));
        tutorProfile.setStatus(TutorProfileStatus.ACTIVE);
        TutorProfile savedTutor = tutorProfileRepository.saveAndFlush(tutorProfile);

        Review review = new Review();
        review.setTutorProfile(savedTutor);
        review.setClientUserId(UUID.randomUUID());
        review.setRating(5);
        review.setText("Спокойно объяснила тему");
        review.setStatus(ReviewStatus.PENDING_MODERATION);
        reviewRepository.saveAndFlush(review);

        var reviews = reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING_MODERATION);

        assertThat(reviews).hasSize(1);
        assertThat(Persistence.getPersistenceUtil().isLoaded(reviews.getFirst().getTutorProfile())).isTrue();
        assertThat(ReviewDto.from(reviews.getFirst()).tutorName()).isEqualTo("Анна Иванова");
    }
}
