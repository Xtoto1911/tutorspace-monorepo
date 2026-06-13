package com.tutor.subscriptionservice.tariff;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions/tariffs")
public class TariffController {

    private final TariffRepository tariffRepository;

    @GetMapping
    public List<TariffDto> listActive() {
        return tariffRepository.findByActiveTrueOrderByPriceRubAsc()
                .stream()
                .map(TariffDto::from)
                .toList();
    }
}
