package com.tutor.subscriptionservice.tariff;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TariffSeedConfig {

    @Bean
    CommandLineRunner seedTariffs(TariffRepository tariffRepository) {
        return args -> {
            seed(tariffRepository, "CRM_BASIC", "CRM Basic", 990, 30);
            seed(tariffRepository, "CRM_PRO", "CRM Pro", 1990, 30);
            seed(tariffRepository, "CRM_PREMIUM", "CRM Premium", 2990, 30);
        };
    }

    private void seed(TariffRepository repository, String code, String name, int priceRub, int durationDays) {
        if (repository.existsByCode(code)) {
            return;
        }
        repository.save(Tariff.builder()
                .code(code)
                .name(name)
                .priceRub(priceRub)
                .durationDays(durationDays)
                .active(true)
                .build());
    }
}
