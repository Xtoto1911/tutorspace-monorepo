package com.tutor.crmservice.favorite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientFavoriteServiceTest {

    @Mock
    private ClientFavoriteRepository clientFavoriteRepository;

    @Test
    void addFavoriteCreatesClientTutorPair() {
        ClientFavoriteService service = new ClientFavoriteService(clientFavoriteRepository);
        UUID clientId = UUID.randomUUID();
        UUID tutorProfileId = UUID.randomUUID();

        when(clientFavoriteRepository.findByClientUserIdAndTutorProfileId(clientId, tutorProfileId))
                .thenReturn(Optional.empty());
        when(clientFavoriteRepository.save(any(ClientFavorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FavoriteToggleResponse response = service.addFavorite(clientId, tutorProfileId);

        ArgumentCaptor<ClientFavorite> captor = ArgumentCaptor.forClass(ClientFavorite.class);
        verify(clientFavoriteRepository).save(captor.capture());
        assertEquals(clientId, captor.getValue().getClientUserId());
        assertEquals(tutorProfileId, captor.getValue().getTutorProfileId());
        assertEquals(tutorProfileId.toString(), response.tutorId());
        assertEquals(true, response.favorite());
    }

    @Test
    void addFavoriteIsIdempotentForExistingPair() {
        ClientFavoriteService service = new ClientFavoriteService(clientFavoriteRepository);
        UUID clientId = UUID.randomUUID();
        UUID tutorProfileId = UUID.randomUUID();
        ClientFavorite existing = new ClientFavorite();
        existing.setClientUserId(clientId);
        existing.setTutorProfileId(tutorProfileId);

        when(clientFavoriteRepository.findByClientUserIdAndTutorProfileId(clientId, tutorProfileId))
                .thenReturn(Optional.of(existing));

        FavoriteToggleResponse response = service.addFavorite(clientId, tutorProfileId);

        verify(clientFavoriteRepository, never()).save(any());
        assertEquals(tutorProfileId.toString(), response.tutorId());
        assertEquals(true, response.favorite());
    }

    @Test
    void removeFavoriteDeletesExistingPair() {
        ClientFavoriteService service = new ClientFavoriteService(clientFavoriteRepository);
        UUID clientId = UUID.randomUUID();
        UUID tutorProfileId = UUID.randomUUID();
        ClientFavorite existing = new ClientFavorite();

        when(clientFavoriteRepository.findByClientUserIdAndTutorProfileId(clientId, tutorProfileId))
                .thenReturn(Optional.of(existing));

        FavoriteToggleResponse response = service.removeFavorite(clientId, tutorProfileId);

        verify(clientFavoriteRepository).delete(existing);
        assertEquals(tutorProfileId.toString(), response.tutorId());
        assertFalse(response.favorite());
    }
}
