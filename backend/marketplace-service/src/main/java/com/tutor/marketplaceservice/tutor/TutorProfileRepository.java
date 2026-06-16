package com.tutor.marketplaceservice.tutor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutorProfileRepository extends JpaRepository<TutorProfile, UUID> {
    Optional<TutorProfile> findBySlug(String slug);
    Optional<TutorProfile> findByUserId(UUID userId);
    boolean existsBySlug(String slug);
    boolean existsByUserId(UUID userId);
    List<TutorProfile> findByStatusOrderByFullNameAsc(TutorProfileStatus status);

    @Query("""
            select distinct profile from TutorProfile profile
            left join profile.subjects subject
            where profile.status = com.tutor.marketplaceservice.tutor.TutorProfileStatus.ACTIVE
              and (:subjectSlug is null or subject.slug = :subjectSlug)
              and (:minExperienceYears is null or profile.experienceYears >= :minExperienceYears)
              and (:minPricePerHour is null or profile.pricePerHour >= :minPricePerHour)
              and (:maxPricePerHour is null or profile.pricePerHour <= :maxPricePerHour)
              and (:minRating is null or profile.rating >= :minRating)
              and (:maxRating is null or profile.rating <= :maxRating)
            order by profile.rating desc, profile.fullName asc
            """)
    List<TutorProfile> searchPublicProfiles(@Param("subjectSlug") String subjectSlug,
                                            @Param("minExperienceYears") Integer minExperienceYears,
                                            @Param("minPricePerHour") BigDecimal minPricePerHour,
                                            @Param("maxPricePerHour") BigDecimal maxPricePerHour,
                                            @Param("minRating") Double minRating,
                                            @Param("maxRating") Double maxRating);
}
