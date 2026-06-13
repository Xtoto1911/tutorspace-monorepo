package com.tutor.crmservice.favorite;

import com.tutor.crmservice.integration.MarketplaceClient;
import com.tutor.crmservice.integration.MarketplaceTutorProfileDto;
import com.tutor.crmservice.view.ClientFavoritesPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientFavoritePageService {

    private static final Locale RU = Locale.forLanguageTag("ru-RU");
    private static final DateTimeFormatter SAVED_AT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", RU)
            .withZone(ZoneId.systemDefault());

    private final ClientFavoriteRepository clientFavoriteRepository;
    private final MarketplaceClient marketplaceClient;

    @Transactional(readOnly = true)
    public ClientFavoritesPageDto getFavoritesPage(UUID clientUserId) {
        List<ClientFavorite> favorites = clientFavoriteRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
        List<UUID> tutorIds = favorites.stream().map(ClientFavorite::getTutorProfileId).toList();
        Map<UUID, MarketplaceTutorProfileDto> profilesById = marketplaceClient.fetchTutorProfilesByIds(tutorIds).stream()
                .collect(Collectors.toMap(MarketplaceTutorProfileDto::id, profile -> profile));

        List<ClientFavoritesPageDto.TutorDto> tutors = favorites.stream()
                .map(favorite -> toTutorDto(favorite, profilesById.get(favorite.getTutorProfileId())))
                .flatMap(List::stream)
                .toList();

        return new ClientFavoritesPageDto(tutors);
    }

    private List<ClientFavoritesPageDto.TutorDto> toTutorDto(ClientFavorite favorite, MarketplaceTutorProfileDto profile) {
        if (profile == null || !"ACTIVE".equals(profile.status())) {
            return List.of();
        }

        String subject = profile.subjects() == null || profile.subjects().isEmpty()
                ? "Предмет уточняется"
                : profile.subjects().getFirst().name();

        return List.of(new ClientFavoritesPageDto.TutorDto(
                profile.id().toString(),
                profile.slug(),
                profile.fullName(),
                subject,
                profile.experienceYears() + " лет опыта",
                String.format(RU, "%.1f", profile.rating() == null ? 0.0 : profile.rating()),
                NumberFormat.getCurrencyInstance(RU).format(profile.pricePerHour()).replace(",00", ""),
                SAVED_AT_FORMAT.format(favorite.getCreatedAt())
        ));
    }
}
