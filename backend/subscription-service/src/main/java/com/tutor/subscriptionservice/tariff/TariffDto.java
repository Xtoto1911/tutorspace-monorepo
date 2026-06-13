package com.tutor.subscriptionservice.tariff;

import java.util.UUID;

public record TariffDto(
        UUID id,
        String code,
        String name,
        int priceRub,
        int durationDays,
        boolean active
) {
    public static TariffDto from(Tariff tariff) {
        return new TariffDto(
                tariff.getId(),
                tariff.getCode(),
                tariff.getName(),
                tariff.getPriceRub(),
                tariff.getDurationDays(),
                tariff.isActive()
        );
    }
}
