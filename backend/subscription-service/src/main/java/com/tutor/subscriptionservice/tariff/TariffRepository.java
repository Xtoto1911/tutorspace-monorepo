package com.tutor.subscriptionservice.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {
    Optional<Tariff> findByCode(String code);
    boolean existsByCode(String code);
    List<Tariff> findByActiveTrueOrderByPriceRubAsc();
}
