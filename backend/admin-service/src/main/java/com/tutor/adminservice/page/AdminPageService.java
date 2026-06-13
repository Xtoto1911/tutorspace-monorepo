package com.tutor.adminservice.page;

import com.tutor.adminservice.audit.AuditLog;
import com.tutor.adminservice.audit.AuditLogRepository;
import com.tutor.adminservice.client.AuthServiceClient;
import com.tutor.adminservice.client.MarketplaceServiceClient;
import com.tutor.adminservice.client.SubscriptionAdminClient;
import com.tutor.adminservice.complaint.Complaint;
import com.tutor.adminservice.complaint.ComplaintRepository;
import com.tutor.adminservice.complaint.ComplaintStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AdminPageService {

    private static final AdminPageDtos.SelectedUser EMPTY_SELECTED_USER = new AdminPageDtos.SelectedUser(
            "--",
            "Нет пользователей",
            "",
            "",
            "Пусто",
            "Нет данных",
            List.of(),
            List.of()
    );

    private final AuthServiceClient authClient;
    private final MarketplaceServiceClient marketplaceClient;
    private final SubscriptionAdminClient subscriptionClient;
    private final ComplaintRepository complaintRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminPageDtos.DashboardPage dashboard(String token) {
        var usersResult = safe(() -> authClient.listUsers(token));
        var complaints = safeList(() -> complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING));
        var audit = recentAudit();
        var snapshotResult = safe(subscriptionClient::snapshot);
        var marketplaceResult = safe(() -> marketplaceClient.listReviews("PENDING_MODERATION"));

        return new AdminPageDtos.DashboardPage(
                List.of(
                        new AdminPageDtos.DashboardMetric("users", "Пользователи", String.valueOf(usersResult.items().orElse(List.of()).size()), "live auth-service", "indigo"),
                        new AdminPageDtos.DashboardMetric("shield", "Жалобы", String.valueOf(complaints.size()), "pending", "amber"),
                        new AdminPageDtos.DashboardMetric(
                                "credit-card",
                                "Подписки",
                                String.valueOf(snapshotResult.items().map(snapshot -> safeSize(snapshot.subscriptions())).orElse(0)),
                                snapshotResult.available() ? "live subscription-service" : "subscription-service недоступен",
                                snapshotResult.available() ? "teal" : "amber"
                        ),
                        new AdminPageDtos.DashboardMetric("activity", "Аудит", String.valueOf(audit.size()), "admin-service", "rose")
                ),
                serviceStatuses(usersResult.available(), marketplaceResult.available(), snapshotResult.available()),
                alerts(usersResult, marketplaceResult, snapshotResult),
                complaints.stream().map(this::moderationQueueItem).toList(),
                List.of(),
                audit.stream().limit(5).map(this::dashboardAuditItem).toList()
        );
    }

    public AdminPageDtos.UsersPage usersPage(String token) {
        var usersResult = safe(() -> authClient.listUsers(token));
        var users = usersResult.items().orElse(List.of());
        var unavailable = !usersResult.available();
        var rows = users.stream().map(this::userRow).toList();
        var selected = rows.isEmpty() ? emptySelectedUser(unavailable) : selectedUser(rows.get(0));
        var admins = users.stream().filter(user -> "ROLE_ADMIN".equals(user.role())).count();
        var metricMeta = unavailable ? "auth-service недоступен" : "live auth-service";

        return new AdminPageDtos.UsersPage(
                List.of(
                        new AdminPageDtos.SimpleMetric("users", String.valueOf(users.size()), "Пользователи", metricMeta, unavailable ? "rose" : "indigo"),
                        new AdminPageDtos.SimpleMetric("shield", String.valueOf(admins), "Администраторы", "role count", "emerald")
                ),
                List.of("Все", "Админы", "Модераторы", "Заблокированные"),
                rows,
                selected,
                userCards(rows.isEmpty(), admins, unavailable)
        );
    }

    public AdminPageDtos.SubscriptionsPage subscriptionsPage() {
        var snapshotResult = safe(subscriptionClient::snapshot);
        var snapshot = snapshotResult.items().orElse(emptySnapshot());
        var subscriptions = safeList(snapshot.subscriptions());
        var tariffs = safeList(snapshot.tariffs());
        var payments = safeList(snapshot.payments());
        var periods = safeList(snapshot.periods());
        var revenuePayments = revenuePayments(payments);
        var paidAmount = paymentTotal(revenuePayments);
        var unavailable = !snapshotResult.available();
        var metricHint = unavailable ? "subscription-service недоступен" : "live subscription-service";
        var paymentHint = unavailable ? "subscription-service недоступен" : "confirmed snapshot";

        return new AdminPageDtos.SubscriptionsPage(
                List.of(
                        new AdminPageDtos.SubscriptionMetric("credit-card", String.valueOf(subscriptions.size()), "Подписки", metricHint, unavailable ? "rose" : "emerald"),
                        new AdminPageDtos.SubscriptionMetric("wallet", String.valueOf(paidAmount), "Платежи, ₽", paymentHint, unavailable ? "rose" : "indigo")
                ),
                subscriptions.stream().map(subscription -> subscriptionRow(subscription, periods, revenuePayments)).toList(),
                revenueByPlan(tariffs, revenuePayments),
                unavailable ? unavailableWebhookQueue() : List.of(),
                unavailable ? List.of() : tariffs.stream().map(this::planSetting).toList()
        );
    }

    public AdminPageDtos.SiteStatsPage siteStats(String token) {
        var usersResult = safe(() -> authClient.listUsers(token));
        var users = usersResult.items().orElse(List.of());
        var complaints = safeList(() -> complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING));
        var snapshotResult = safe(subscriptionClient::snapshot);
        var snapshot = snapshotResult.items().orElse(emptySnapshot());
        var subscriptions = safeList(snapshot.subscriptions());
        var paidAmount = paymentTotal(revenuePayments(safeList(snapshot.payments())));
        var audit = recentAudit();
        var subscriptionMeta = snapshotResult.available() ? "live subscription-service" : "subscription-service недоступен";
        var financeCaption = snapshotResult.available()
                ? "Платежные итоги доступны в subscription-service"
                : "subscription-service недоступен";
        var financeTrend = snapshotResult.available() ? "0%" : "unavailable";

        return new AdminPageDtos.SiteStatsPage(
                List.of(
                        new AdminPageDtos.SiteMetric(
                                "users",
                                String.valueOf(users.size()),
                                "Пользователи",
                                usersResult.available() ? "live auth-service" : "auth-service недоступен",
                                usersResult.available() ? "indigo" : "amber"
                        ),
                        new AdminPageDtos.SiteMetric("credit-card", String.valueOf(subscriptions.size()), "Подписки", subscriptionMeta, snapshotResult.available() ? "emerald" : "amber"),
                        new AdminPageDtos.SiteMetric("shield", String.valueOf(complaints.size()), "Жалобы", "pending", "amber"),
                        new AdminPageDtos.SiteMetric("activity", String.valueOf(audit.size()), "События", "audit logs", "cyan")
                ),
                List.of("Обзор", "Пользователи", "Модерация"),
                new AdminPageDtos.GrowthChart("Рост", "Доступны только текущие агрегаты", List.of(), List.of(), List.of(), "История не хранится"),
                new AdminPageDtos.Conversion("Конверсия", "0%", "Нет текущего источника конверсий"),
                List.of(),
                List.of(),
                new AdminPageDtos.FinanceSummary("Выручка", paidAmount + " ₽", financeCaption, financeTrend),
                List.of(),
                audit.stream().limit(5).map(this::siteEvent).toList(),
                new AdminPageDtos.SiteNote("Данные", "Показаны только доступные live агрегаты.", "Обновить")
        );
    }

    public AdminPageDtos.AuditPage auditPage() {
        var logs = recentAudit();
        var events = logs.stream().map(this::auditEvent).toList();
        var selected = logs.isEmpty() ? emptySelectedAudit() : selectedAudit(logs.get(0));

        return new AdminPageDtos.AuditPage(
                List.of(new AdminPageDtos.AuditMetric("activity", String.valueOf(logs.size()), "События", "admin-service", "indigo")),
                List.of("Все", "Пользователи", "Модерация", "Система"),
                events,
                selected,
                List.of(),
                List.of()
        );
    }

    public AdminPageDtos.SettingsPage settings(String token) {
        var usersResult = safe(() -> authClient.listUsers(token));
        var snapshotResult = safe(subscriptionClient::snapshot);
        var marketplaceResult = safe(() -> marketplaceClient.listTutorProfiles("ACTIVE"));
        var users = usersResult.items().orElse(List.of());
        var admins = users.stream().filter(user -> "ROLE_ADMIN".equals(user.role())).count();
        var moderators = users.stream().filter(user -> "ROLE_MODERATOR".equals(user.role())).count();
        var authHint = usersResult.available() ? "live auth-service" : "auth-service недоступен";

        return new AdminPageDtos.SettingsPage(
                List.of(new AdminPageDtos.SettingsMetric("settings", String.valueOf(users.size()), "Пользователи", authHint, usersResult.available() ? "indigo" : "rose")),
                List.of(
                        new AdminPageDtos.SecuritySetting("JWT", "Проверка токенов включена в admin-service.", true),
                        new AdminPageDtos.SecuritySetting("RBAC", "Доступ зависит от ролей auth-service.", true)
                ),
                List.of(
                        new AdminPageDtos.RoleSetting("Администраторы", "ROLE_ADMIN", String.valueOf(admins)),
                        new AdminPageDtos.RoleSetting("Модераторы", "ROLE_MODERATOR", String.valueOf(moderators))
                ),
                List.of(),
                List.of(
                        settingsService("auth-service", usersResult.available(), "Пользователи и роли"),
                        settingsService("marketplace-service", marketplaceResult.available(), "Профили и отзывы"),
                        settingsService("subscription-service", snapshotResult.available(), "CRM подписки")
                ),
                List.of(new AdminPageDtos.PolicySetting("Тестовые платежи", "YooKassa должна оставаться на тестовых учетных данных.", List.of("payments", "test")))
        );
    }

    public AdminPageDtos.ModeratorReviewPage moderatorReviewPage() {
        var reviews = safeReviews();
        var profiles = safeProfiles();
        var rows = new java.util.ArrayList<AdminPageDtos.ModeratorProfileRow>();
        rows.addAll(reviews.stream().map(this::moderatorReviewRow).toList());
        rows.addAll(profiles.stream().map(this::moderatorProfileRow).toList());
        var total = rows.size();
        var selected = rows.isEmpty() ? emptyModeratorSelectedProfile() : selectedModeratorProfile(rows.get(0));

        return new AdminPageDtos.ModeratorReviewPage(
                List.of(new AdminPageDtos.ModeratorMetric("shield", String.valueOf(total), "в очереди", "live marketplace-service", String.valueOf(total), total == 0 ? "slate" : "amber")),
                List.of("Все", "Анкеты", "Отзывы"),
                rows,
                selected,
                List.of(new AdminPageDtos.ModeratorPipeline("Всего", String.valueOf(total), total == 0 ? 0 : 100)),
                List.of("Проверить публичное описание", "Не публиковать спорные отзывы"),
                new AdminPageDtos.ModeratorDecision("Решение модератора", "Действие применяется только к поддерживаемой сущности.", List.of("live", "audit"))
        );
    }

    public AdminPageDtos.ModeratorDashboard moderatorDashboard() {
        var complaints = safeList(() -> complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING));
        var reviews = safeReviews();
        var profiles = safeProfiles();
        var decisions = recentAudit().stream()
                .filter(log -> log.getActionType() != null && log.getActionType().startsWith("MODERATION_"))
                .limit(8)
                .map(log -> new AdminPageDtos.ModeratorRecentDecision(
                        stringId(log.getId()),
                        value(log.getActionType()),
                        value(log.getTargetType()),
                        stringId(log.getTargetId()),
                        time(log.getCreatedAt())
                ))
                .toList();

        return new AdminPageDtos.ModeratorDashboard(
                complaints.size(),
                reviews.size(),
                profiles.size(),
                complaints.size() + reviews.size() + profiles.size(),
                decisions
        );
    }

    private List<AuditLog> recentAudit() {
        return safeList(auditLogRepository::findAll).stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    private AdminPageDtos.ModerationQueueItem moderationQueueItem(Complaint complaint) {
        return new AdminPageDtos.ModerationQueueItem(
                stringId(complaint.getId()),
                value(complaint.getTargetType()),
                stringId(complaint.getTargetId()),
                value(complaint.getStatus()),
                stringId(complaint.getReportedUserId())
        );
    }

    private AdminPageDtos.DashboardAuditItem dashboardAuditItem(AuditLog log) {
        return new AdminPageDtos.DashboardAuditItem(
                stringId(log.getId()),
                stringId(log.getAdminUserId()),
                log.getActionType(),
                time(log.getCreatedAt())
        );
    }

    private AdminPageDtos.UserRow userRow(AuthServiceClient.AdminUserDto user) {
        var email = user.email() == null ? "" : user.email();
        var name = email.isBlank() ? stringId(user.id()) : email.substring(0, email.indexOf("@") > 0 ? email.indexOf("@") : email.length());
        return new AdminPageDtos.UserRow(
                stringId(user.id()),
                initials(name),
                name,
                email,
                value(user.role()),
                value(user.status()),
                "Low",
                userTone(user.status()),
                false
        );
    }

    private AdminPageDtos.SelectedUser selectedUser(AdminPageDtos.UserRow row) {
        return new AdminPageDtos.SelectedUser(
                row.initials(),
                row.name(),
                row.email(),
                row.role(),
                row.status(),
                "Низкий риск",
                List.of(new AdminPageDtos.UserSignal("Источник", "auth-service")),
                userActions(row)
        );
    }

    private List<String> userActions(AdminPageDtos.UserRow row) {
        if ("ROLE_ADMIN".equals(row.role())) {
            return List.of();
        }

        var statusAction = "BLOCKED".equalsIgnoreCase(row.status()) ? "Разблокировать" : "Заблокировать";
        return List.of(statusAction, "Сделать админом");
    }

    private AdminPageDtos.SelectedUser emptySelectedUser(boolean unavailable) {
        if (!unavailable) {
            return EMPTY_SELECTED_USER;
        }
        return new AdminPageDtos.SelectedUser(
                "--",
                "Нет пользователей",
                "",
                "",
                "Недоступно",
                "auth-service недоступен",
                List.of(),
                List.of()
        );
    }

    private AdminPageDtos.UserCards userCards(boolean empty, long admins, boolean unavailable) {
        if (empty) {
            return new AdminPageDtos.UserCards(
                    new AdminPageDtos.InfoCard(
                            unavailable ? "auth-service недоступен" : "Нет пользователей",
                            unavailable ? "Не удалось получить список пользователей." : "Auth-service вернул пустой список.",
                            ""
                    ),
                    new AdminPageDtos.RoleCard("Роли", "0", "Нет назначенных ролей.", ""),
                    new AdminPageDtos.InfoCard("Модерация", "Нет данных.", ""),
                    new AdminPageDtos.InfoCard("Заметка", "Демо-данные не используются.", "")
            );
        }
        return new AdminPageDtos.UserCards(
                new AdminPageDtos.InfoCard("Аудит", "События доступны на странице аудита.", "Открыть"),
                new AdminPageDtos.RoleCard("Админы", String.valueOf(admins), "Количество ROLE_ADMIN.", ""),
                new AdminPageDtos.InfoCard("Модерация", "Действия выполняются отдельными endpoints.", ""),
                new AdminPageDtos.InfoCard("Заметка", "Список получен из auth-service.", "")
        );
    }

    private AdminPageDtos.SubscriptionRow subscriptionRow(
            SubscriptionAdminClient.SubscriptionItem subscription,
            List<SubscriptionAdminClient.PeriodItem> periods,
            List<SubscriptionAdminClient.PaymentItem> payments
    ) {
        return new AdminPageDtos.SubscriptionRow(
                stringId(subscription.id()),
                stringId(subscription.tutorUserId()),
                planName(subscription.tariffCode()),
                value(subscription.status()),
                time(subscription.activeUntil()),
                subscriptionPayment(subscription, periods, payments),
                subscriptionTone(subscription.status())
        );
    }

    private String subscriptionPayment(
            SubscriptionAdminClient.SubscriptionItem subscription,
            List<SubscriptionAdminClient.PeriodItem> periods,
            List<SubscriptionAdminClient.PaymentItem> payments
    ) {
        var payment = periods.stream()
                .filter(period -> sameValue(period.subscriptionId(), subscription.id()))
                .max(Comparator.comparing(
                        this::periodSortTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .flatMap(period -> payments.stream()
                        .filter(candidate -> sameValue(candidate.id(), period.paymentId()))
                        .findFirst());
        return payment.map(item -> item.amountRub() + " ₽").orElse("0 ₽");
    }

    private Instant periodSortTime(SubscriptionAdminClient.PeriodItem period) {
        return period.activeUntil() == null ? period.startedAt() : period.activeUntil();
    }

    private List<AdminPageDtos.PlanShare> revenueByPlan(
            List<SubscriptionAdminClient.TariffItem> tariffs,
            List<SubscriptionAdminClient.PaymentItem> payments
    ) {
        var total = paymentTotal(payments);
        var amountsByCode = new java.util.LinkedHashMap<String, Integer>();
        payments.forEach(payment -> amountsByCode.merge(payment.tariffCode(), payment.amountRub(), Integer::sum));

        var shares = tariffs.stream()
                .map(tariff -> planShare(tariff.code(), tariff.name(), amountsByCode.remove(tariff.code()), total))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        amountsByCode.forEach((code, amount) -> shares.add(planShare(code, null, amount, total)));
        return shares;
    }

    private List<AdminPageDtos.WebhookItem> unavailableWebhookQueue() {
        return List.of(new AdminPageDtos.WebhookItem(
                "subscription-service",
                "subscription-service недоступен",
                "Повторить чтение позже",
                "rose"
        ));
    }

    private AdminPageDtos.PlanShare planShare(String code, String displayName, Integer amount, int total) {
        var planAmount = amount == null ? 0 : amount;
        var percent = total == 0 ? 0 : (int) Math.round(planAmount * 100.0 / total);
        return new AdminPageDtos.PlanShare(displayName(displayName, code), planAmount + " ₽", percent, planTone(code));
    }

    private AdminPageDtos.PlanSetting planSetting(SubscriptionAdminClient.TariffItem tariff) {
        return new AdminPageDtos.PlanSetting(planName(tariff.code()), tariff.priceRub() + " ₽", tariff.durationDays() + " дней");
    }

    private AdminPageDtos.SiteEvent siteEvent(AuditLog log) {
        return new AdminPageDtos.SiteEvent(time(log.getCreatedAt()), log.getActionType(), log.getTargetType(), "indigo");
    }

    private AdminPageDtos.AuditEvent auditEvent(AuditLog log) {
        return new AdminPageDtos.AuditEvent(
                stringId(log.getId()),
                time(log.getCreatedAt()),
                stringId(log.getAdminUserId()),
                "admin",
                log.getActionType(),
                log.getTargetType() + ":" + stringId(log.getTargetId()),
                "admin-service",
                "low",
                "recorded",
                "indigo",
                false
        );
    }

    private AdminPageDtos.SelectedAudit selectedAudit(AuditLog log) {
        return new AdminPageDtos.SelectedAudit(
                stringId(log.getId()),
                log.getActionType(),
                stringId(log.getAdminUserId()),
                log.getTargetType() + " " + stringId(log.getTargetId()),
                List.of(
                        new AdminPageDtos.AuditMeta("service", "admin-service"),
                        new AdminPageDtos.AuditMeta("time", time(log.getCreatedAt()))
                ),
                log.getDetails() == null || log.getDetails().isBlank() ? List.of() : List.of(log.getDetails())
        );
    }

    private AdminPageDtos.SelectedAudit emptySelectedAudit() {
        return new AdminPageDtos.SelectedAudit(
                "",
                "Нет событий",
                "",
                "Журнал аудита пуст.",
                List.of(),
                List.of()
        );
    }

    private AdminPageDtos.SettingsService settingsService(String name, boolean available, String description) {
        return new AdminPageDtos.SettingsService(name, available ? "online" : "unavailable", description, available ? "emerald" : "rose");
    }

    private List<MarketplaceServiceClient.ReviewModerationDto> safeReviews() {
        return safeList(() -> marketplaceClient.listReviews("PENDING_MODERATION"));
    }

    private List<MarketplaceServiceClient.TutorProfileModerationDto> safeProfiles() {
        return safeList(() -> marketplaceClient.listTutorProfiles("HIDDEN"));
    }

    private AdminPageDtos.ModeratorProfileRow moderatorReviewRow(MarketplaceServiceClient.ReviewModerationDto review) {
        var author = review.authorName() == null || review.authorName().isBlank() ? "Отзыв клиента" : review.authorName();
        var tutorName = review.tutorName() == null || review.tutorName().isBlank() ? "преподавателе" : review.tutorName();
        var rating = review.rating() == null ? 0 : review.rating();
        return new AdminPageDtos.ModeratorProfileRow(
                stringId(review.id()),
                "REVIEW",
                initials(tutorName),
                "Отзыв о " + tutorName,
                "Отзыв",
                "Отзыв",
                value(review.status()),
                review.text() == null || review.text().isBlank() ? "Текст отзыва не заполнен" : author + ": " + review.text(),
                Math.max(0, Math.min(100, rating * 20)) + "%",
                stringId(review.tutorProfileId()),
                "amber",
                true
        );
    }

    private AdminPageDtos.ModeratorProfileRow moderatorProfileRow(MarketplaceServiceClient.TutorProfileModerationDto profile) {
        var name = profile.fullName() == null || profile.fullName().isBlank() ? "Анкета преподавателя" : profile.fullName();
        return new AdminPageDtos.ModeratorProfileRow(
                stringId(profile.id()),
                "TUTOR_PROFILE",
                initials(name),
                name,
                subjectNames(profile.subjects()),
                "Анкета",
                value(profile.status()),
                profile.description() == null || profile.description().isBlank() ? "Описание не заполнено" : profile.description(),
                profile.rating() == null ? "0%" : Math.round(Math.max(0, Math.min(5, profile.rating())) * 20) + "%",
                stringId(profile.userId()),
                "emerald",
                false
        );
    }

    private AdminPageDtos.ModeratorSelectedProfile selectedModeratorProfile(AdminPageDtos.ModeratorProfileRow row) {
        return new AdminPageDtos.ModeratorSelectedProfile(
                row.id(),
                row.status(),
                row.initials(),
                row.name(),
                row.subject(),
                row.type(),
                "Средний",
                List.of(new AdminPageDtos.ModeratorCheckItem("Проверить содержимое", false)),
                row.score(),
                row.summary()
        );
    }

    private AdminPageDtos.ModeratorSelectedProfile emptyModeratorSelectedProfile() {
        return new AdminPageDtos.ModeratorSelectedProfile(
                "Нет анкет",
                "Пусто",
                "--",
                "Очередь пуста",
                "Нет данных",
                "-",
                "Низкий",
                List.of(),
                "0%",
                "Нет профилей для проверки."
        );
    }

    private String subjectNames(List<MarketplaceServiceClient.SubjectDto> subjects) {
        var names = safeList(subjects).stream()
                .map(MarketplaceServiceClient.SubjectDto::name)
                .filter(name -> name != null && !name.isBlank())
                .toList();
        return names.isEmpty() ? "Профиль" : String.join(", ", names);
    }

    private List<AdminPageDtos.ServiceStatus> serviceStatuses(boolean authAvailable, boolean marketplaceAvailable, boolean subscriptionAvailable) {
        return List.of(
                serviceStatus("auth-service", authAvailable),
                serviceStatus("marketplace-service", marketplaceAvailable),
                serviceStatus("subscription-service", subscriptionAvailable)
        );
    }

    private AdminPageDtos.ServiceStatus serviceStatus(String name, boolean available) {
        return new AdminPageDtos.ServiceStatus(name, available ? "online" : "unavailable", available ? "live" : "n/a", available ? "live" : "n/a", available ? "green" : "red");
    }

    private List<AdminPageDtos.Alert> alerts(
            SafeResult<List<AuthServiceClient.AdminUserDto>> users,
            SafeResult<List<MarketplaceServiceClient.ReviewModerationDto>> marketplace,
            SafeResult<SubscriptionAdminClient.Snapshot> snapshot
    ) {
        return Map.of(
                        "auth-service", users.available(),
                        "marketplace-service", marketplace.available(),
                        "subscription-service", snapshot.available()
                )
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> new AdminPageDtos.Alert(entry.getKey(), entry.getKey() + " недоступен", "Раздел показан с пустыми live данными.", "medium", time(Instant.now())))
                .toList();
    }

    private SubscriptionAdminClient.Snapshot emptySnapshot() {
        return new SubscriptionAdminClient.Snapshot(List.of(), List.of(), List.of(), List.of());
    }

    private List<SubscriptionAdminClient.PaymentItem> revenuePayments(List<SubscriptionAdminClient.PaymentItem> payments) {
        return payments.stream().filter(this::isConfirmedPayment).toList();
    }

    private boolean isConfirmedPayment(SubscriptionAdminClient.PaymentItem payment) {
        if (payment.status() == null) {
            return false;
        }
        return switch (payment.status().toUpperCase(Locale.ROOT)) {
            case "CONFIRMED", "SUCCEEDED", "SUCCESS", "PAID", "CAPTURED", "COMPLETED" -> true;
            default -> false;
        };
    }

    private int paymentTotal(List<SubscriptionAdminClient.PaymentItem> payments) {
        return payments.stream().mapToInt(SubscriptionAdminClient.PaymentItem::amountRub).sum();
    }

    private boolean sameValue(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private <T> SafeResult<T> safe(Supplier<T> supplier) {
        try {
            return new SafeResult<>(Optional.ofNullable(supplier.get()), true);
        } catch (RuntimeException exception) {
            return new SafeResult<>(Optional.empty(), false);
        }
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier) {
        return safe(supplier).items().orElse(List.of());
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private int safeSize(List<?> items) {
        return items == null ? 0 : items.size();
    }

    private String planName(String code) {
        if (code == null || code.isBlank()) {
            return "Free";
        }
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CRM_BASIC" -> "CRM Basic";
            case "CRM_PRO" -> "CRM Pro";
            case "CRM_PREMIUM" -> "CRM Premium";
            default -> code;
        };
    }

    private String displayName(String displayName, String code) {
        return displayName == null || displayName.isBlank() ? planName(code) : displayName;
    }

    private String planTone(String code) {
        if ("CRM_PRO".equalsIgnoreCase(code)) {
            return "indigo";
        }
        if ("CRM_PREMIUM".equalsIgnoreCase(code)) {
            return "violet";
        }
        return "slate";
    }

    private String subscriptionTone(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "emerald";
        }
        if ("PAST_DUE".equalsIgnoreCase(status)) {
            return "amber";
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return "rose";
        }
        return "slate";
    }

    private String userTone(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "emerald";
        }
        if ("BLOCKED".equalsIgnoreCase(status)) {
            return "rose";
        }
        return "indigo";
    }

    private String initials(String value) {
        if (value == null || value.isBlank()) {
            return "--";
        }
        var parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String stringId(Object value) {
        return value == null ? "" : value.toString();
    }

    private String time(Instant instant) {
        return instant == null ? "-" : instant.toString();
    }

    private record SafeResult<T>(Optional<T> items, boolean available) {}
}
