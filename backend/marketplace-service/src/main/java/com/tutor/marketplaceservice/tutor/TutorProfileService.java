package com.tutor.marketplaceservice.tutor;

import com.tutor.marketplaceservice.review.AdminComplaintClient;
import com.tutor.marketplaceservice.shared.ConflictException;
import com.tutor.marketplaceservice.subject.Subject;
import com.tutor.marketplaceservice.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorProfileService {

    private final TutorProfileRepository tutorProfileRepository;
    private final SubjectService subjectService;
    private final AdminComplaintClient adminComplaintClient;

    @Transactional(readOnly = true)
    public List<TutorProfileDto> search(String subjectSlug,
                                        Integer minExperienceYears,
                                        BigDecimal maxPricePerHour) {
        return tutorProfileRepository.searchPublicProfiles(subjectSlug, minExperienceYears, maxPricePerHour)
                .stream()
                .map(TutorProfileDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TutorProfileDto> listByStatus(TutorProfileStatus status) {
        return tutorProfileRepository.findByStatusOrderByFullNameAsc(status)
                .stream()
                .map(TutorProfileDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TutorProfile getProfile(UUID tutorProfileId) {
        return tutorProfileRepository.findById(tutorProfileId)
                .orElseThrow(() -> new TutorProfileNotFoundException(tutorProfileId.toString()));
    }

    @Transactional(readOnly = true)
    public TutorProfileDto getProfileByIdOrSlug(String idOrSlug) {
        return TutorProfileDto.from(findByIdOrSlug(idOrSlug));
    }

    @Transactional
    public void updateStatus(UUID tutorProfileId, TutorProfileStatus status) {
        TutorProfile profile = getProfile(tutorProfileId);
        profile.setStatus(status);
        tutorProfileRepository.save(profile);
    }

    @Transactional
    public TutorProfileDto create(CreateTutorProfileRequest request) {
        if (tutorProfileRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Tutor slug already exists: " + request.slug());
        }

        if (tutorProfileRepository.existsByUserId(request.userId())) {
            throw new ConflictException("Tutor profile already exists for user: " + request.userId());
        }

        TutorProfile profile = new TutorProfile();
        profile.setUserId(request.userId());
        profile.setSlug(request.slug());
        profile.setFullName(request.fullName());
        profile.setDescription(request.description());
        profile.setExperienceYears(request.experienceYears());
        profile.setPricePerHour(request.pricePerHour());
        profile.setPhotoUrl(request.photoUrl());
        profile.setCity(normalizeNullable(request.city()));
        profile.setEducation(copyValues(request.education()));
        profile.setHighlights(copyValues(request.highlights()));
        profile.setTags(copyValues(request.tags()));
        profile.setResponseTimeMinutes(request.responseTimeMinutes());
        profile.setStatus(TutorProfileStatus.HIDDEN);
        profile.setSubjects(resolveSubjects(request.subjectIds()));

        return TutorProfileDto.from(tutorProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public TutorProfileComplaintDto createComplaint(String idOrSlug,
                                                    UUID reporterUserId,
                                                    CreateTutorProfileComplaintRequest request) {
        TutorProfile profile = findByIdOrSlug(idOrSlug);
        AdminComplaintClient.AdminComplaintResponse response = adminComplaintClient.createComplaint(
                new AdminComplaintClient.AdminComplaintRequest(
                        profile.getUserId(),
                        reporterUserId,
                        "FAKE_PROFILE",
                        request.reason(),
                        request.description(),
                        "MEDIUM",
                        "TUTOR_PROFILE",
                        profile.getId(),
                        createProfileSnapshot(profile)
                )
        );
        return new TutorProfileComplaintDto(response.id(), response.status());
    }

    @Transactional
    public TutorProfileDto syncFromCrmSettings(SyncTutorProfileRequest request) {
        TutorProfile profile = tutorProfileRepository.findByUserId(request.userId())
                .orElseGet(() -> createProfileShell(request));

        if (hasText(request.fullName())) {
            profile.setFullName(request.fullName().trim());
        }
        if (hasText(request.description())) {
            profile.setDescription(request.description().trim());
        } else if (hasText(request.headline())) {
            profile.setDescription(request.headline().trim());
        }
        if (request.experienceYears() != null) {
            profile.setExperienceYears(Math.max(request.experienceYears(), 0));
        }
        if (request.pricePerHour() != null) {
            profile.setPricePerHour(request.pricePerHour());
        }
        profile.setStatus(request.hidden() ? TutorProfileStatus.HIDDEN : TutorProfileStatus.ACTIVE);
        if (hasText(request.city())) {
            profile.setCity(request.city().trim());
        }

        if (request.subjects() != null && request.subjects().stream().anyMatch(this::hasText)) {
            Set<Subject> subjects = subjectService.resolveSubjectsByNamesOrDefault(request.subjects());
            if (!subjects.isEmpty()) {
                profile.setSubjects(new LinkedHashSet<>(subjects));
            }
        }

        return TutorProfileDto.from(tutorProfileRepository.save(profile));
    }

    @Transactional
    public void hideProfile(String idOrSlug) {
        TutorProfile profile = findByIdOrSlug(idOrSlug);
        profile.setStatus(TutorProfileStatus.HIDDEN);
        tutorProfileRepository.save(profile);
    }

    private TutorProfile findByIdOrSlug(String idOrSlug) {
        try {
            return tutorProfileRepository.findById(UUID.fromString(idOrSlug))
                    .orElseThrow(() -> new TutorProfileNotFoundException(idOrSlug));
        } catch (IllegalArgumentException ex) {
            return tutorProfileRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new TutorProfileNotFoundException(idOrSlug));
        }
    }

    private TutorProfile createProfileShell(SyncTutorProfileRequest request) {
        TutorProfile profile = new TutorProfile();
        profile.setUserId(request.userId());
        profile.setSlug(createUniqueSlug(request.fullName(), request.userId()));
        profile.setFullName(valueOrDefault(request.fullName(), "Репетитор TutorSpace"));
        profile.setDescription(valueOrDefault(
                request.description(),
                valueOrDefault(request.headline(), "Профиль заполняется.")
        ));
        profile.setExperienceYears(request.experienceYears() == null ? 0 : Math.max(request.experienceYears(), 0));
        profile.setPricePerHour(request.pricePerHour() == null
                ? new BigDecimal("1500.00")
                : request.pricePerHour());
        return profile;
    }

    private String createUniqueSlug(String fullName, UUID userId) {
        String base = transliterate(valueOrDefault(fullName, "tutor"))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
                .replaceAll("^-|-$", "");

        if (base.isBlank()) {
            base = "tutor";
        }

        String suffix = userId.toString().substring(0, 8);
        String slug = base + "-" + suffix;
        int counter = 2;
        while (tutorProfileRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix + "-" + counter;
            counter++;
        }
        return slug;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> copyValues(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String createProfileSnapshot(TutorProfile profile) {
        return "Профиль репетитора: " + valueOrDefault(profile.getFullName(), "Без имени")
                + ". Описание: " + valueOrDefault(profile.getDescription(), "Без описания")
                + ". Опыт: " + profile.getExperienceYears()
                + ". Цена: " + profile.getPricePerHour();
    }

    private String transliterate(String value) {
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            result.append(switch (Character.toLowerCase(ch)) {
                case 'а' -> "a";
                case 'б' -> "b";
                case 'в' -> "v";
                case 'г' -> "g";
                case 'д' -> "d";
                case 'е', 'ё' -> "e";
                case 'ж' -> "zh";
                case 'з' -> "z";
                case 'и', 'й' -> "i";
                case 'к' -> "k";
                case 'л' -> "l";
                case 'м' -> "m";
                case 'н' -> "n";
                case 'о' -> "o";
                case 'п' -> "p";
                case 'р' -> "r";
                case 'с' -> "s";
                case 'т' -> "t";
                case 'у' -> "u";
                case 'ф' -> "f";
                case 'х' -> "h";
                case 'ц' -> "c";
                case 'ч' -> "ch";
                case 'ш' -> "sh";
                case 'щ' -> "sch";
                case 'ы' -> "y";
                case 'э' -> "e";
                case 'ю' -> "yu";
                case 'я' -> "ya";
                case 'ь', 'ъ' -> "";
                default -> String.valueOf(ch);
            });
        }
        return result.toString();
    }

    private Set<Subject> resolveSubjects(Set<UUID> subjectIds) {
        Set<Subject> subjects = new LinkedHashSet<>();
        subjectIds.forEach(subjectId -> subjects.add(subjectService.getSubject(subjectId)));
        return subjects;
    }
}
