package com.tutor.marketplaceservice.tutor;

import com.tutor.marketplaceservice.subject.Subject;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tutor_profiles")
@Getter
@Setter
@NoArgsConstructor
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "city", length = 120)
    private String city;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tutor_profile_education",
            joinColumns = @JoinColumn(name = "tutor_profile_id")
    )
    @OrderColumn(name = "education_order")
    @Column(name = "education_text", nullable = false, length = 500)
    @BatchSize(size = 50)
    private List<String> education = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tutor_profile_highlights",
            joinColumns = @JoinColumn(name = "tutor_profile_id")
    )
    @OrderColumn(name = "highlight_order")
    @Column(name = "highlight_text", nullable = false, length = 500)
    @BatchSize(size = 50)
    private List<String> highlights = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tutor_profile_tags",
            joinColumns = @JoinColumn(name = "tutor_profile_id")
    )
    @OrderColumn(name = "tag_order")
    @Column(name = "tag", nullable = false, length = 120)
    @BatchSize(size = 50)
    private List<String> tags = new ArrayList<>();

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    @Column(nullable = false)
    private Integer experienceYears;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    private String photoUrl;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(nullable = false)
    private Integer reviewsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TutorProfileStatus status = TutorProfileStatus.HIDDEN;

    @ManyToMany
    @JoinTable(
            name = "tutor_profile_subjects",
            joinColumns = @JoinColumn(name = "tutor_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @BatchSize(size = 50)
    private Set<Subject> subjects = new LinkedHashSet<>();
}
