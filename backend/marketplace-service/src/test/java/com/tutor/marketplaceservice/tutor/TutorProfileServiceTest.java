package com.tutor.marketplaceservice.tutor;

import com.tutor.marketplaceservice.review.AdminComplaintClient;
import com.tutor.marketplaceservice.shared.ConflictException;
import com.tutor.marketplaceservice.subject.Subject;
import com.tutor.marketplaceservice.subject.SubjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorProfileServiceTest {

    @Mock
    private TutorProfileRepository tutorProfileRepository;

    @Mock
    private SubjectService subjectService;

    @Mock
    private AdminComplaintClient adminComplaintClient;

    @InjectMocks
    private TutorProfileService tutorProfileService;

    @Test
    void createBuildsTutorProfileWithSubjects() {
        UUID subjectId = UUID.randomUUID();
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("Math");
        subject.setSlug("math");
        subject.setDescription("Алгебра, геометрия и подготовка к экзаменам.");

        when(tutorProfileRepository.existsBySlug("ivan-math")).thenReturn(false);
        when(tutorProfileRepository.existsByUserId(any(UUID.class))).thenReturn(false);
        when(subjectService.getSubject(subjectId)).thenReturn(subject);
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileDto dto = tutorProfileService.create(new CreateTutorProfileRequest(
                UUID.randomUUID(),
                "ivan-math",
                "Ivan Petrov",
                "Math tutor",
                5,
                new BigDecimal("1500.00"),
                "https://example.com/photo.jpg",
                "Москва",
                List.of("МГУ"),
                List.of("Подготовил 30 выпускников"),
                List.of("ЕГЭ"),
                15,
                Set.of(subjectId)
        ));

        assertThat(dto.slug()).isEqualTo("ivan-math");
        assertThat(dto.status()).isEqualTo(TutorProfileStatus.HIDDEN);
        assertThat(dto.subjects()).hasSize(1);
        assertThat(dto.subjects().getFirst().slug()).isEqualTo("math");
        assertThat(dto.subjects().getFirst().description())
                .isEqualTo("Алгебра, геометрия и подготовка к экзаменам.");
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    void dtoExposesCompletePublicProfileWithoutInventedMetrics() {
        TutorProfile profile = new TutorProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(UUID.randomUUID());
        profile.setSlug("anna-math");
        profile.setFullName("Анна Иванова");
        profile.setDescription("Готовлю к ОГЭ и ЕГЭ");
        profile.setExperienceYears(8);
        profile.setPricePerHour(new BigDecimal("2100.00"));
        profile.setCity("Москва");
        profile.setEducation(List.of("МГУ, механико-математический факультет"));
        profile.setHighlights(List.of("Средний результат учеников на ЕГЭ — 84 балла"));
        profile.setTags(List.of("ОГЭ", "ЕГЭ", "Олимпиады"));
        profile.setResponseTimeMinutes(20);

        TutorProfileDto dto = TutorProfileDto.from(profile);

        assertThat(dto.city()).isEqualTo("Москва");
        assertThat(dto.education()).containsExactly("МГУ, механико-математический факультет");
        assertThat(dto.highlights()).containsExactly("Средний результат учеников на ЕГЭ — 84 балла");
        assertThat(dto.tags()).containsExactly("ОГЭ", "ЕГЭ", "Олимпиады");
        assertThat(dto.responseTimeMinutes()).isEqualTo(20);
        assertThat(dto.activeStudents()).isNull();
        assertThat(dto.lessonsCount()).isNull();
        assertThat(dto.repeatRatePercent()).isNull();
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(tutorProfileRepository.existsBySlug("ivan-math")).thenReturn(true);

        CreateTutorProfileRequest request = new CreateTutorProfileRequest(
                UUID.randomUUID(),
                "ivan-math",
                "Ivan Petrov",
                "Math tutor",
                5,
                new BigDecimal("1500.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of(UUID.randomUUID())
        );

        assertThatThrownBy(() -> tutorProfileService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ivan-math");
    }

    @Test
    void syncFromCrmSettingsCreatesActiveProfileForRegisteredTutor() {
        UUID userId = UUID.randomUUID();
        Subject subject = new Subject();
        subject.setId(UUID.randomUUID());
        subject.setName("Математика");
        subject.setSlug("matematika");

        when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(tutorProfileRepository.existsBySlug("anna-ivanova-" + userId.toString().substring(0, 8))).thenReturn(false);
        when(subjectService.resolveSubjectsByNamesOrDefault(List.of("Математика"))).thenReturn(Set.of(subject));
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileDto dto = tutorProfileService.syncFromCrmSettings(new SyncTutorProfileRequest(
                userId,
                "Анна Иванова",
                "Казань",
                "Готовлю к ОГЭ",
                "Понятные занятия",
                4,
                new BigDecimal("1500.00"),
                false,
                List.of("Математика")
        ));

        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.fullName()).isEqualTo("Анна Иванова");
        assertThat(dto.status()).isEqualTo(TutorProfileStatus.ACTIVE);
        assertThat(dto.subjects()).extracting(subjectDto -> subjectDto.slug()).containsExactly("matematika");
    }

    @Test
    void syncFromCrmSettingsHidesExistingMarketplaceProfile() {
        UUID userId = UUID.randomUUID();
        TutorProfile profile = new TutorProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setSlug("anna-ivanova");
        profile.setFullName("Анна Иванова");
        profile.setDescription("Описание");
        profile.setExperienceYears(4);
        profile.setPricePerHour(new BigDecimal("1500.00"));
        profile.setStatus(TutorProfileStatus.ACTIVE);

        when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(subjectService.resolveSubjectsByNamesOrDefault(List.of("Математика"))).thenReturn(Set.of());
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileDto dto = tutorProfileService.syncFromCrmSettings(new SyncTutorProfileRequest(
                userId,
                "Анна Иванова",
                "Казань",
                "Готовлю к ОГЭ",
                "Понятные занятия",
                4,
                new BigDecimal("1500.00"),
                true,
                List.of("Математика")
        ));

        assertThat(dto.status()).isEqualTo(TutorProfileStatus.HIDDEN);
    }

    @Test
    void syncFromCrmSettingsUpdatesCityWithoutErasingPublicCollections() {
        UUID userId = UUID.randomUUID();
        TutorProfile profile = new TutorProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setSlug("anna-ivanova");
        profile.setFullName("Анна Иванова");
        profile.setDescription("Описание");
        profile.setExperienceYears(4);
        profile.setPricePerHour(new BigDecimal("1500.00"));
        profile.setCity("Самара");
        profile.setEducation(List.of("Самарский университет"));
        profile.setHighlights(List.of("Подготовила 40 выпускников"));
        profile.setTags(List.of("ОГЭ", "ЕГЭ"));
        profile.setResponseTimeMinutes(30);
        profile.setStatus(TutorProfileStatus.ACTIVE);

        when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(subjectService.resolveSubjectsByNamesOrDefault(List.of("Математика"))).thenReturn(Set.of());
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileDto dto = tutorProfileService.syncFromCrmSettings(new SyncTutorProfileRequest(
                userId,
                "Анна Иванова",
                "Казань",
                "Готовлю к ОГЭ",
                "Понятные занятия",
                4,
                new BigDecimal("1500.00"),
                false,
                List.of("Математика")
        ));

        assertThat(dto.city()).isEqualTo("Казань");
        assertThat(dto.education()).containsExactly("Самарский университет");
        assertThat(dto.highlights()).containsExactly("Подготовила 40 выпускников");
        assertThat(dto.tags()).containsExactly("ОГЭ", "ЕГЭ");
        assertThat(dto.responseTimeMinutes()).isEqualTo(30);
    }

    @Test
    void syncFromCrmSettingsPreservesExistingValuesWhenCrmOmitsThem() {
        UUID userId = UUID.randomUUID();
        Subject subject = new Subject();
        subject.setId(UUID.randomUUID());
        subject.setName("Математика");
        subject.setSlug("matematika");
        subject.setDescription("Подготовка к экзаменам.");

        TutorProfile profile = new TutorProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setSlug("anna-ivanova");
        profile.setFullName("Анна Иванова");
        profile.setDescription("Авторское описание");
        profile.setExperienceYears(9);
        profile.setPricePerHour(new BigDecimal("2300.00"));
        profile.setCity("Самара");
        profile.setSubjects(Set.of(subject));
        profile.setStatus(TutorProfileStatus.ACTIVE);

        when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileDto dto = tutorProfileService.syncFromCrmSettings(new SyncTutorProfileRequest(
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        ));

        assertThat(dto.fullName()).isEqualTo("Анна Иванова");
        assertThat(dto.description()).isEqualTo("Авторское описание");
        assertThat(dto.experienceYears()).isEqualTo(9);
        assertThat(dto.pricePerHour()).isEqualByComparingTo("2300.00");
        assertThat(dto.city()).isEqualTo("Самара");
        assertThat(dto.subjects()).extracting(subjectDto -> subjectDto.slug()).containsExactly("matematika");
    }

    @Test
    void createComplaintForwardsTutorProfileSnapshotToAdminService() {
        UUID tutorProfileId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        UUID reporterUserId = UUID.randomUUID();
        UUID complaintId = UUID.randomUUID();
        TutorProfile profile = new TutorProfile();
        profile.setId(tutorProfileId);
        profile.setUserId(tutorUserId);
        profile.setSlug("anna-math");
        profile.setFullName("Анна Иванова");
        profile.setDescription("Готовлю к ОГЭ");
        profile.setExperienceYears(4);
        profile.setPricePerHour(new BigDecimal("1500.00"));
        profile.setStatus(TutorProfileStatus.ACTIVE);

        when(tutorProfileRepository.findBySlug("anna-math")).thenReturn(Optional.of(profile));
        when(adminComplaintClient.createComplaint(any())).thenReturn(new AdminComplaintClient.AdminComplaintResponse(
                complaintId,
                "PENDING"
        ));

        TutorProfileComplaintDto dto = tutorProfileService.createComplaint(
                "anna-math",
                reporterUserId,
                new CreateTutorProfileComplaintRequest("Фейковый профиль", "Нет подтверждения опыта")
        );

        assertThat(dto.id()).isEqualTo(complaintId);
        assertThat(dto.status()).isEqualTo("PENDING");
        verify(adminComplaintClient).createComplaint(argThat(request ->
                request.reportedUserId().equals(tutorUserId)
                        && request.reporterUserId().equals(reporterUserId)
                        && "FAKE_PROFILE".equals(request.category())
                        && "TUTOR_PROFILE".equals(request.targetType())
                        && request.targetId().equals(tutorProfileId)
                        && request.targetSnapshot().contains("Анна Иванова")
                        && request.targetSnapshot().contains("Готовлю к ОГЭ")
        ));
    }

    @Test
    void hideProfileMarksExistingProfileHidden() {
        UUID tutorProfileId = UUID.randomUUID();
        TutorProfile profile = new TutorProfile();
        profile.setId(tutorProfileId);
        profile.setUserId(UUID.randomUUID());
        profile.setSlug("anna-math");
        profile.setFullName("Анна Иванова");
        profile.setDescription("Готовлю к ОГЭ");
        profile.setExperienceYears(4);
        profile.setPricePerHour(new BigDecimal("1500.00"));
        profile.setStatus(TutorProfileStatus.ACTIVE);

        when(tutorProfileRepository.findById(tutorProfileId)).thenReturn(Optional.of(profile));
        when(tutorProfileRepository.save(profile)).thenReturn(profile);

        tutorProfileService.hideProfile(tutorProfileId.toString());

        assertThat(profile.getStatus()).isEqualTo(TutorProfileStatus.HIDDEN);
        verify(tutorProfileRepository).save(profile);
    }

    @Test
    void updateStatusPersistsModeratorDecision() {
        UUID tutorProfileId = UUID.randomUUID();
        TutorProfile profile = new TutorProfile();
        profile.setId(tutorProfileId);
        profile.setStatus(TutorProfileStatus.HIDDEN);
        when(tutorProfileRepository.findById(tutorProfileId)).thenReturn(Optional.of(profile));
        when(tutorProfileRepository.save(profile)).thenReturn(profile);

        tutorProfileService.updateStatus(tutorProfileId, TutorProfileStatus.ACTIVE);

        assertThat(profile.getStatus()).isEqualTo(TutorProfileStatus.ACTIVE);
        verify(tutorProfileRepository).save(profile);
    }
}
