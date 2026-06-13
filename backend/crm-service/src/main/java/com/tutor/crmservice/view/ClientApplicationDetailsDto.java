package com.tutor.crmservice.view;

import java.util.List;

public record ClientApplicationDetailsDto(
        String id,
        String tutorName,
        String tutorSlug,
        String status,
        String statusTone,
        String subject,
        List<String> chips,
        String note,
        String price,
        List<InfoRowDto> parameters
) {
    public record InfoRowDto(String label, String value) {
    }
}
