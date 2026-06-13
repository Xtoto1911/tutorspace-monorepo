package com.tutor.subscriptionservice.subscription;

import com.tutor.subscriptionservice.tariff.TariffRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/admin/subscriptions")
public class AdminSubscriptionSnapshotController {

    private final TariffRepository tariffRepository;
    private final TutorSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final String internalAdminToken;

    public AdminSubscriptionSnapshotController(
            TariffRepository tariffRepository,
            TutorSubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            SubscriptionPeriodRepository periodRepository,
            @Value("${internal.admin-token:}") String internalAdminToken
    ) {
        this.tariffRepository = tariffRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.periodRepository = periodRepository;
        this.internalAdminToken = internalAdminToken;
    }

    @GetMapping
    public AdminSubscriptionSnapshotDto snapshot(
            @RequestHeader(name = "X-Internal-Token", required = false) String internalToken
    ) {
        verifyInternalToken(internalToken);
        return new AdminSubscriptionSnapshotDto(
                tariffRepository.findAll().stream()
                        .map(AdminSubscriptionSnapshotDto.TariffItem::from)
                        .toList(),
                subscriptionRepository.findAll().stream()
                        .map(AdminSubscriptionSnapshotDto.SubscriptionItem::from)
                        .toList(),
                paymentRepository.findAll().stream()
                        .map(AdminSubscriptionSnapshotDto.PaymentItem::from)
                        .toList(),
                periodRepository.findAll().stream()
                        .map(AdminSubscriptionSnapshotDto.PeriodItem::from)
                        .toList()
        );
    }

    private void verifyInternalToken(String internalToken) {
        if (internalAdminToken == null || internalAdminToken.isBlank() || internalToken == null) {
            throw invalidInternalToken();
        }

        var expectedBytes = internalAdminToken.getBytes(StandardCharsets.UTF_8);
        var actualBytes = internalToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw invalidInternalToken();
        }
    }

    private ResponseStatusException invalidInternalToken() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
    }
}
