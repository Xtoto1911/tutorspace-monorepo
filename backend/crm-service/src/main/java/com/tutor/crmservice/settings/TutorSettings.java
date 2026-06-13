package com.tutor.crmservice.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tutor_settings")
@Getter
@Setter
@NoArgsConstructor
public class TutorSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String profileName;
    private String profileCity;
    private String profileHeadline;
    @Column(length = 2000)
    private String profileAbout;
    private Integer experienceYears = 0;
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerHour = new BigDecimal("1500.00");

    @ElementCollection
    @CollectionTable(name = "tutor_subjects", joinColumns = @JoinColumn(name = "tutor_settings_id"))
    @Column(name = "subject")
    private List<String> subjects = new ArrayList<>();

    private boolean isHidden = true;

    @ElementCollection
    @CollectionTable(name = "tutor_visibility", joinColumns = @JoinColumn(name = "tutor_settings_id"))
    private List<ToggleItem> visibility = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tutor_notifications", joinColumns = @JoinColumn(name = "tutor_settings_id"))
    private List<ToggleItem> notifications = new ArrayList<>();
}
