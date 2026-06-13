package com.tutor.crmservice.favorite;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientFavoriteService {

    private final ClientFavoriteRepository clientFavoriteRepository;

    @Transactional
    public FavoriteToggleResponse addFavorite(UUID clientUserId, UUID tutorProfileId) {
        clientFavoriteRepository.findByClientUserIdAndTutorProfileId(clientUserId, tutorProfileId)
                .orElseGet(() -> {
                    ClientFavorite favorite = new ClientFavorite();
                    favorite.setClientUserId(clientUserId);
                    favorite.setTutorProfileId(tutorProfileId);
                    return clientFavoriteRepository.save(favorite);
                });

        return new FavoriteToggleResponse(tutorProfileId.toString(), true);
    }

    @Transactional
    public FavoriteToggleResponse removeFavorite(UUID clientUserId, UUID tutorProfileId) {
        clientFavoriteRepository.findByClientUserIdAndTutorProfileId(clientUserId, tutorProfileId)
                .ifPresent(clientFavoriteRepository::delete);

        return new FavoriteToggleResponse(tutorProfileId.toString(), false);
    }
}
