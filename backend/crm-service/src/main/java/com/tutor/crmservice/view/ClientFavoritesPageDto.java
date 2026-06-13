package com.tutor.crmservice.view;

import java.util.List;

public record ClientFavoritesPageDto(List<TutorDto> tutors) {

    public record TutorDto(
            String id,
            String slug,
            String name,
            String subject,
            String meta,
            String rating,
            String price,
            String savedAt
    ) {
    }
}
