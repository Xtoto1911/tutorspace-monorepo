package com.tutor.crmservice.settings;

import com.tutor.crmservice.integration.MarketplaceClient;
import com.tutor.crmservice.view.ClientSettingsPageDto;
import com.tutor.crmservice.view.TutorSettingsPageDto;
import com.tutor.crmservice.view.TutorWorkspaceProfilePageDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

    private final TutorSettingsRepository tutorSettingsRepository = mock(TutorSettingsRepository.class);
    private final ClientSettingsRepository clientSettingsRepository = mock(ClientSettingsRepository.class);
    private final MarketplaceClient marketplaceClient = mock(MarketplaceClient.class);
    private final SettingsService service = new SettingsService(tutorSettingsRepository, clientSettingsRepository, marketplaceClient);

    @Test
    void getTutorSettings_copiesEntityCollectionsBeforeReturningDto() {
        UUID tutorId = UUID.randomUUID();
        TutorSettings settings = createTutorSettings(tutorId);
        when(tutorSettingsRepository.findByUserId(tutorId)).thenReturn(Optional.of(settings));
        when(marketplaceClient.fetchAvailableSubjects()).thenReturn(List.of("Математика", "Физика"));

        TutorSettingsPageDto dto = service.getTutorSettings(tutorId);

        assertEquals(List.of("Математика"), dto.subjects());
        assertNotSame(settings.getSubjects(), dto.subjects());
    }

    @Test
    void getTutorWorkspaceProfile_copiesPublicCardChipsBeforeReturningDto() {
        UUID tutorId = UUID.randomUUID();
        TutorSettings settings = createTutorSettings(tutorId);
        when(tutorSettingsRepository.findByUserId(tutorId)).thenReturn(Optional.of(settings));

        TutorWorkspaceProfilePageDto dto = service.getTutorWorkspaceProfile(tutorId);

        assertEquals(List.of("Математика"), dto.publicCard().chips());
        assertNotSame(settings.getSubjects(), dto.publicCard().chips());
    }

    @Test
    void getTutorSettings_createsHiddenDefaultsForNewTutor() {
        UUID tutorId = UUID.randomUUID();
        when(tutorSettingsRepository.findByUserId(tutorId)).thenReturn(Optional.empty());
        when(tutorSettingsRepository.save(any(TutorSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(marketplaceClient.fetchAvailableSubjects()).thenReturn(List.of("Математика", "Физика"));

        TutorSettingsPageDto dto = service.getTutorSettings(tutorId);

        assertTrue(dto.isHidden());
        assertEquals("Профиль скрыт", dto.profile().moderationStatus());
        assertEquals("Профиль скрыт", dto.moderation().status());
    }

    @Test
    void getTutorWorkspaceProfile_createsHiddenDefaultsForNewTutorOnboarding() {
        UUID tutorId = UUID.randomUUID();
        when(tutorSettingsRepository.findByUserId(tutorId)).thenReturn(Optional.empty());
        when(tutorSettingsRepository.save(any(TutorSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutorWorkspaceProfilePageDto dto = service.getTutorWorkspaceProfile(tutorId);

        assertEquals("Скрыт из каталога", dto.identity().status());
        assertEquals("Карточка скрыта", dto.publicCard().title());
    }

    @Test
    void getClientSettings_usesLiveEmailAndDoesNotReturnDemoStudents() {
        UUID clientId = UUID.randomUUID();
        when(clientSettingsRepository.findByUserId(clientId)).thenReturn(Optional.empty());
        when(clientSettingsRepository.save(any(ClientSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientSettingsPageDto dto = service.getClientSettings(clientId, "client.e2e@tutorspace.local");

        assertEquals("client.e2e@tutorspace.local", dto.profile().email());
        assertEquals(List.of(), dto.students());
        assertEquals(List.of(), dto.searchPreferences());
        assertEquals("0", dto.metrics().get(1).value());
    }

    private TutorSettings createTutorSettings(UUID tutorId) {
        TutorSettings settings = new TutorSettings();
        settings.setUserId(tutorId);
        settings.setProfileName("Анна Петрова");
        settings.setProfileCity("Москва");
        settings.setProfileHeadline("Готовлю к ЕГЭ");
        settings.setProfileAbout("Понятная система прогресса.");
        settings.setExperienceYears(7);
        settings.setPricePerHour(new BigDecimal("2100.00"));
        settings.setSubjects(new ArrayList<>(List.of("Математика")));
        settings.setHidden(false);
        settings.setVisibility(new ArrayList<>(List.of(createToggle("Показывать заявки", "Описание", true))));
        settings.setNotifications(new ArrayList<>(List.of(createToggle("Новые заявки", "Описание", true))));
        return settings;
    }

    private ToggleItem createToggle(String title, String description, boolean enabled) {
        ToggleItem item = new ToggleItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setEnabled(enabled);
        return item;
    }
}
