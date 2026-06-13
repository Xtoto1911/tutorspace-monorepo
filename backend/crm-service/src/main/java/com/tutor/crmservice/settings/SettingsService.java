package com.tutor.crmservice.settings;

import com.tutor.crmservice.integration.MarketplaceClient;
import com.tutor.crmservice.view.ClientSettingsPageDto;
import com.tutor.crmservice.view.TutorSettingsPageDto;
import com.tutor.crmservice.view.TutorWorkspaceProfilePageDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SettingsService {

    private final TutorSettingsRepository tutorSettingsRepository;
    private final ClientSettingsRepository clientSettingsRepository;
    private final MarketplaceClient marketplaceClient;

    public SettingsService(TutorSettingsRepository tutorSettingsRepository,
                           ClientSettingsRepository clientSettingsRepository,
                           MarketplaceClient marketplaceClient) {
        this.tutorSettingsRepository = tutorSettingsRepository;
        this.clientSettingsRepository = clientSettingsRepository;
        this.marketplaceClient = marketplaceClient;
    }

    @Transactional(readOnly = true)
    public TutorSettingsPageDto getTutorSettings(UUID userId) {
        TutorSettings settings = tutorSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultTutorSettings(userId));
        return toTutorDto(settings);
    }

    @Transactional
    public TutorSettingsPageDto updateTutorSettings(UUID userId, TutorSettingsPageDto dto) {
        TutorSettings settings = tutorSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultTutorSettings(userId));

        if (dto.profile() != null) {
            settings.setProfileName(dto.profile().name());
            settings.setProfileCity(dto.profile().city());
            settings.setProfileHeadline(dto.profile().headline());
            settings.setProfileAbout(dto.profile().about());
            if (dto.profile().experienceYears() != null) {
                settings.setExperienceYears(Math.max(dto.profile().experienceYears(), 0));
            }
            if (dto.profile().pricePerHour() != null) {
                settings.setPricePerHour(dto.profile().pricePerHour());
            }
        }
        if (dto.subjects() != null) {
            settings.setSubjects(new ArrayList<>(dto.subjects()));
        }
        settings.setHidden(dto.isHidden());
        if (dto.visibility() != null) {
            settings.setVisibility(new ArrayList<>(dto.visibility().stream().map(this::toToggleItem).toList()));
        }
        if (dto.notifications() != null) {
            settings.setNotifications(new ArrayList<>(dto.notifications().stream().map(this::toToggleItem).toList()));
        }

        TutorSettings saved = tutorSettingsRepository.save(settings);
        syncMarketplace(saved);
        return toTutorDto(saved);
    }

    @Transactional(readOnly = true)
    public TutorWorkspaceProfilePageDto getTutorWorkspaceProfile(UUID userId) {
        TutorSettings settings = tutorSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultTutorSettings(userId));
        return toWorkspaceDto(settings);
    }

    @Transactional
    public TutorWorkspaceProfilePageDto updateTutorWorkspaceProfile(UUID userId, TutorWorkspaceProfilePageDto dto) {
        TutorSettings settings = tutorSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultTutorSettings(userId));

        if (dto.identity() != null) {
            settings.setProfileName(dto.identity().name());
            settings.setProfileCity(dto.identity().city());
            if (dto.identity().experienceYears() != null) {
                settings.setExperienceYears(Math.max(dto.identity().experienceYears(), 0));
            }
            if (dto.identity().pricePerHour() != null) {
                settings.setPricePerHour(BigDecimal.valueOf(dto.identity().pricePerHour()));
            }
        }
        if (dto.fields() != null) {
            dto.fields().forEach(field -> {
                if ("name".equals(field.id())) {
                    settings.setProfileName(field.value());
                } else if ("city".equals(field.id())) {
                    settings.setProfileCity(field.value());
                } else if ("headline".equals(field.id())) {
                    settings.setProfileHeadline(field.value());
                } else if ("about".equals(field.id())) {
                    settings.setProfileAbout(field.value());
                } else if ("experienceYears".equals(field.id())) {
                    settings.setExperienceYears(parseNonNegativeInt(field.value(), settings.getExperienceYears()));
                } else if ("pricePerHour".equals(field.id())) {
                    settings.setPricePerHour(parsePrice(field.value(), settings.getPricePerHour()));
                }
            });
        }
        if (dto.subjects() != null) {
            settings.setSubjects(new ArrayList<>(dto.subjects().stream().map(TutorWorkspaceProfilePageDto.SubjectDto::title).toList()));
        }

        TutorSettings saved = tutorSettingsRepository.save(settings);
        syncMarketplace(saved);
        return toWorkspaceDto(saved);
    }

    @Transactional(readOnly = true)
    public ClientSettingsPageDto getClientSettings(UUID userId) {
        return getClientSettings(userId, null);
    }

    @Transactional(readOnly = true)
    public ClientSettingsPageDto getClientSettings(UUID userId, String email) {
        ClientSettings settings = clientSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultClientSettings(userId, email));
        if (!hasText(settings.getEmail()) && hasText(email)) {
            settings.setEmail(email.trim());
        }
        return toClientDto(settings);
    }

    @Transactional
    public ClientSettingsPageDto updateClientSettings(UUID userId, ClientSettingsPageDto dto) {
        ClientSettings settings = clientSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultClientSettings(userId, null));

        if (dto.profile() != null) {
            settings.setParentName(dto.profile().parentName());
            settings.setPhone(dto.profile().phone());
            settings.setEmail(dto.profile().email());
            settings.setCity(dto.profile().city());
            settings.setTimezone(dto.profile().timezone());
            settings.setLanguage(dto.profile().language());
        }
        if (dto.notifications() != null) {
            settings.setNotifications(new ArrayList<>(dto.notifications().stream().map(this::toToggleItem).toList()));
        }
        if (dto.privacy() != null) {
            settings.setPrivacy(new ArrayList<>(dto.privacy().stream().map(this::toToggleItem).toList()));
        }

        ClientSettings saved = clientSettingsRepository.save(settings);
        return toClientDto(saved);
    }

    private TutorSettings createDefaultTutorSettings(UUID userId) {
        TutorSettings settings = new TutorSettings();
        settings.setUserId(userId);
        settings.setProfileName("");
        settings.setProfileCity("");
        settings.setProfileHeadline("");
        settings.setProfileAbout("");
        settings.setSubjects(new ArrayList<>());
        settings.setHidden(true);
        settings.setExperienceYears(0);
        settings.setPricePerHour(new BigDecimal("1500.00"));
        settings.setVisibility(new ArrayList<>(List.of(
                createToggle("quick-requests", "Разрешить быстрые заявки", "Клиенты смогут отправить заявку без предварительного чата.", true)
        )));
        settings.setNotifications(new ArrayList<>(List.of(
                createToggle("messages", "Сообщения", "Новые сообщения от родителей и учеников.", true),
                createToggle("requests", "Заявки", "Новые заявки и изменения статусов.", true),
                createToggle("calendar", "Календарь", "Напоминания о занятиях и переносах.", false)
        )));
        return tutorSettingsRepository.save(settings);
    }

    private ClientSettings createDefaultClientSettings(UUID userId, String email) {
        ClientSettings settings = new ClientSettings();
        settings.setUserId(userId);
        settings.setParentName("");
        settings.setPhone("");
        settings.setEmail(hasText(email) ? email.trim() : "");
        settings.setCity("");
        settings.setTimezone("Europe/Moscow");
        settings.setLanguage("Русский");
        settings.setNotifications(new ArrayList<>(List.of(
                createToggle("tutor-replies", "Ответы репетиторов", "когда репетитор написал по заявке", true),
                createToggle("lesson-reminders", "Напоминания о занятиях", "за 24 часа и за 1 час до урока", true),
                createToggle("favorites-updates", "Изменения избранных", "скидки, свободные окна, обновления", false)
        )));
        settings.setPrivacy(new ArrayList<>(List.of(
                createToggle("hide-phone", "Скрывать телефон", "показывать номер только после отклика", true),
                createToggle("analytics", "Разрешить аналитику", "помогает улучшать подбор репетиторов", true),
                createToggle("two-factor", "Двухфакторная защита", "подтверждение входа кодом", false)
        )));
        return clientSettingsRepository.save(settings);
    }

    private TutorSettingsPageDto toTutorDto(TutorSettings settings) {
        List<String> availableSubjects = marketplaceClient.fetchAvailableSubjects();
        List<String> subjects = new ArrayList<>(settings.getSubjects());
        return new TutorSettingsPageDto(
                new TutorSettingsPageDto.ProfileDto(
                        settings.getProfileName(),
                        settings.getProfileCity(),
                        settings.getProfileHeadline(),
                        settings.getProfileAbout(),
                        getInitials(settings.getProfileName()),
                        settings.isHidden() ? "Профиль скрыт" : "Аватар и описание проверены",
                        settings.getExperienceYears(),
                        settings.getPricePerHour()
                ),
                subjects,
                availableSubjects,
                settings.isHidden(),
                settings.getVisibility().stream().map(v ->
                        new TutorSettingsPageDto.ToggleDto(v.getTitle(), v.getDescription(), v.isEnabled())
                ).toList(),
                settings.getNotifications().stream().map(n ->
                        new TutorSettingsPageDto.ToggleDto(n.getTitle(), n.getDescription(), n.isEnabled())
                ).toList(),
                new TutorSettingsPageDto.ModerationDto(
                        settings.isHidden() ? "Профиль скрыт" : "Профиль опубликован",
                        settings.isHidden()
                                ? "Карточка не показывается в каталоге. Заполните профиль и откройте видимость, когда будете готовы."
                                : "Изменения описания могут попасть на повторную проверку модератором."
                )
        );
    }

    private TutorWorkspaceProfilePageDto toWorkspaceDto(TutorSettings settings) {
        int completion = calculateCompletion(settings);
        String price = settings.getPricePerHour() == null ? "1500 ₽/час" : settings.getPricePerHour().stripTrailingZeros().toPlainString() + " ₽/час";
        List<String> subjects = new ArrayList<>(settings.getSubjects());
        List<TutorWorkspaceProfilePageDto.SubjectDto> subjectRows = subjects.stream()
                .map(subject -> new TutorWorkspaceProfilePageDto.SubjectDto(
                        subject,
                        (settings.getExperienceYears() == null ? 0 : settings.getExperienceYears()) + " лет опыта",
                        price,
                        "indigo"
                ))
                .toList();

        return new TutorWorkspaceProfilePageDto(
                List.of(
                        new TutorWorkspaceProfilePageDto.StatDto("✓", completion + "%", "заполнение", "данные из CRM", "emerald"),
                        new TutorWorkspaceProfilePageDto.StatDto("👁", settings.isHidden() ? "скрыт" : "виден", "витрина", "управляется настройками", "indigo"),
                        new TutorWorkspaceProfilePageDto.StatDto("₽", price, "ставка", "публичная карточка", "amber"),
                        new TutorWorkspaceProfilePageDto.StatDto("↻", "CRM", "источник", "без mock-данных", "cyan")
                ),
                new TutorWorkspaceProfilePageDto.IdentityDto(
                        settings.getProfileName(),
                        settings.getProfileHeadline() == null || settings.getProfileHeadline().isBlank() ? "Репетитор TutorSpace" : settings.getProfileHeadline(),
                        settings.getProfileCity(),
                        getInitials(settings.getProfileName()),
                        !settings.isHidden(),
                        settings.isHidden() ? "Скрыт из каталога" : "Профиль опубликован",
                        completion,
                        settings.getExperienceYears(),
                        settings.getPricePerHour() == null ? 1500 : settings.getPricePerHour().intValue()
                ),
                new TutorWorkspaceProfilePageDto.PublicCardDto(
                        settings.isHidden() ? "Карточка скрыта" : "Карточка в каталоге",
                        "Витрина TutorSpace",
                        "Новый профиль",
                        price,
                        "ответ уточняется",
                        "CRM",
                        subjects
                ),
                List.of(
                        new TutorWorkspaceProfilePageDto.FieldDto("name", "ФИО", nullToEmpty(settings.getProfileName()), "input"),
                        new TutorWorkspaceProfilePageDto.FieldDto("city", "Город", nullToEmpty(settings.getProfileCity()), "input"),
                        new TutorWorkspaceProfilePageDto.FieldDto("headline", "Заголовок", nullToEmpty(settings.getProfileHeadline()), "input"),
                        new TutorWorkspaceProfilePageDto.FieldDto("experienceYears", "Опыт, лет", String.valueOf(settings.getExperienceYears() == null ? 0 : settings.getExperienceYears()), "input"),
                        new TutorWorkspaceProfilePageDto.FieldDto("pricePerHour", "Стоимость часа", settings.getPricePerHour() == null ? "1500" : settings.getPricePerHour().stripTrailingZeros().toPlainString(), "input"),
                        new TutorWorkspaceProfilePageDto.FieldDto("about", "О себе", nullToEmpty(settings.getProfileAbout()), "textarea")
                ),
                subjectRows,
                List.of(
                        new TutorWorkspaceProfilePageDto.ChecklistDto("ФИО и город", "Нужны для карточки в каталоге", hasText(settings.getProfileName()) && hasText(settings.getProfileCity())),
                        new TutorWorkspaceProfilePageDto.ChecklistDto("Описание", "Расскажите о подходе к занятиям", hasText(settings.getProfileAbout())),
                        new TutorWorkspaceProfilePageDto.ChecklistDto("Предметы", "Выберите хотя бы одно направление", !subjects.isEmpty())
                )
        );
    }

    private ClientSettingsPageDto toClientDto(ClientSettings settings) {
        int completion = calculateClientCompletion(settings);
        long enabledNotifications = settings.getNotifications().stream().filter(ToggleItem::isEnabled).count();
        long enabledPrivacy = settings.getPrivacy().stream().filter(ToggleItem::isEnabled).count();
        return new ClientSettingsPageDto(
                List.of(
                        new ClientSettingsPageDto.MetricDto("✓", completion + "%", "заполнение профиля", nextClientProfileStep(settings), "emerald"),
                        new ClientSettingsPageDto.MetricDto("👤", "0", "учеников в профиле", "появятся после сохранения карточек учеников", "indigo"),
                        new ClientSettingsPageDto.MetricDto("🔔", String.valueOf(enabledNotifications), "уведомлений включено", "настраивается в live CRM", "amber"),
                        new ClientSettingsPageDto.MetricDto("🛡", String.valueOf(enabledPrivacy), "настроек приватности", "без демо-данных", "violet")
                ),
                new ClientSettingsPageDto.ProfileDto(
                        settings.getParentName(),
                        settings.getPhone(),
                        settings.getEmail(),
                        settings.getCity(),
                        settings.getTimezone(),
                        settings.getLanguage()
                ),
                List.of(),
                settings.getNotifications().stream().map(n ->
                        new ClientSettingsPageDto.ToggleDto(n.getToggleId(), n.getTitle(), n.getDescription(), n.isEnabled())
                ).toList(),
                settings.getPrivacy().stream().map(p ->
                        new ClientSettingsPageDto.ToggleDto(p.getToggleId(), p.getTitle(), p.getDescription(), p.isEnabled())
                ).toList(),
                List.of()
        );
    }

    private int calculateClientCompletion(ClientSettings settings) {
        int done = 0;
        done += hasText(settings.getParentName()) ? 1 : 0;
        done += hasText(settings.getPhone()) ? 1 : 0;
        done += hasText(settings.getEmail()) ? 1 : 0;
        done += hasText(settings.getCity()) ? 1 : 0;
        done += hasText(settings.getTimezone()) ? 1 : 0;
        done += hasText(settings.getLanguage()) ? 1 : 0;
        return Math.round((done * 100f) / 6f);
    }

    private String nextClientProfileStep(ClientSettings settings) {
        if (!hasText(settings.getParentName())) return "добавьте имя родителя";
        if (!hasText(settings.getPhone())) return "добавьте телефон";
        if (!hasText(settings.getCity())) return "укажите город";
        return "основные поля заполнены";
    }

    private ToggleItem toToggleItem(TutorSettingsPageDto.ToggleDto dto) {
        ToggleItem t = new ToggleItem();
        t.setTitle(dto.title());
        t.setDescription(dto.description());
        t.setEnabled(dto.enabled());
        return t;
    }

    private ToggleItem toToggleItem(ClientSettingsPageDto.ToggleDto dto) {
        ToggleItem t = new ToggleItem();
        t.setToggleId(dto.id());
        t.setTitle(dto.title());
        t.setDescription(dto.description());
        t.setEnabled(dto.enabled());
        return t;
    }

    private ToggleItem createToggle(String id, String title, String description, boolean enabled) {
        ToggleItem t = new ToggleItem();
        t.setToggleId(id);
        t.setTitle(title);
        t.setDescription(description);
        t.setEnabled(enabled);
        return t;
    }

    private void syncMarketplace(TutorSettings settings) {
        marketplaceClient.syncTutorProfile(
                settings.getUserId(),
                settings.getProfileName(),
                settings.getProfileCity(),
                settings.getProfileHeadline(),
                settings.getProfileAbout(),
                settings.getExperienceYears(),
                settings.getPricePerHour(),
                settings.isHidden(),
                settings.getSubjects()
        );
    }

    private int calculateCompletion(TutorSettings settings) {
        int done = 0;
        done += hasText(settings.getProfileName()) ? 1 : 0;
        done += hasText(settings.getProfileCity()) ? 1 : 0;
        done += hasText(settings.getProfileHeadline()) ? 1 : 0;
        done += hasText(settings.getProfileAbout()) ? 1 : 0;
        done += settings.getSubjects().isEmpty() ? 0 : 1;
        return done * 20;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Integer parseNonNegativeInt(String value, Integer fallback) {
        try {
            return Math.max(Integer.parseInt(value), 0);
        } catch (Exception ignored) {
            return fallback == null ? 0 : fallback;
        }
    }

    private BigDecimal parsePrice(String value, BigDecimal fallback) {
        try {
            BigDecimal parsed = new BigDecimal(value.replace("₽", "").replace("/час", "").trim());
            return parsed.compareTo(BigDecimal.ZERO) > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback == null ? new BigDecimal("1500.00") : fallback;
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.toString();
    }
}
