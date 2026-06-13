package com.tutor.crmservice.favorite;

import com.tutor.crmservice.integration.MarketplaceClient;
import com.tutor.crmservice.integration.MarketplaceTutorProfileDto;
import com.tutor.crmservice.view.ClientFavoritesPageDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientFavoritePageServiceTest {

    @Test
    void getFavoritesReturnsOnlyVisibleMarketplaceProfilesWithoutDeletingHiddenFavorites() {
        ClientFavoriteRepository repository = mock(ClientFavoriteRepository.class);
        MarketplaceClient marketplaceClient = mock(MarketplaceClient.class);
        ClientFavoritePageService service = new ClientFavoritePageService(repository, marketplaceClient);
        UUID clientId = UUID.randomUUID();
        UUID visibleTutorId = UUID.randomUUID();
        UUID hiddenTutorId = UUID.randomUUID();
        ClientFavorite visibleFavorite = favorite(clientId, visibleTutorId, Instant.parse("2026-05-06T10:00:00Z"));
        ClientFavorite hiddenFavorite = favorite(clientId, hiddenTutorId, Instant.parse("2026-05-06T11:00:00Z"));

        when(repository.findByClientUserIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of(hiddenFavorite, visibleFavorite));
        when(marketplaceClient.fetchTutorProfilesByIds(List.of(hiddenTutorId, visibleTutorId)))
                .thenReturn(List.of(
                        profile(hiddenTutorId, "hidden-tutor", "Скрытый Репетитор", "HIDDEN"),
                        profile(visibleTutorId, "visible-tutor", "Анна Смирнова", "ACTIVE")
                ));

        ClientFavoritesPageDto page = service.getFavoritesPage(clientId);

        assertEquals(1, page.tutors().size());
        assertEquals(visibleTutorId.toString(), page.tutors().getFirst().id());
        assertEquals("Анна Смирнова", page.tutors().getFirst().name());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).delete(any());
    }

    private ClientFavorite favorite(UUID clientId, UUID tutorProfileId, Instant createdAt) {
        ClientFavorite favorite = new ClientFavorite();
        favorite.setClientUserId(clientId);
        favorite.setTutorProfileId(tutorProfileId);
        favorite.setCreatedAt(createdAt);
        return favorite;
    }

    private MarketplaceTutorProfileDto profile(UUID id, String slug, String name, String status) {
        return new MarketplaceTutorProfileDto(
                id,
                UUID.randomUUID(),
                slug,
                name,
                "Подготовка к экзаменам",
                7,
                new BigDecimal("1800.00"),
                null,
                4.9,
                12,
                status,
                List.of(new MarketplaceTutorProfileDto.SubjectDto(UUID.randomUUID(), "Математика", "matematika"))
        );
    }
}
