package com.tutor.adminservice.page;

import java.util.List;

public final class AdminPageDtos {

    private AdminPageDtos() {
    }

    public record DashboardMetric(String icon, String label, String value, String delta, String tone) {}

    public record ServiceStatus(String name, String status, String uptime, String latency, String tone) {}

    public record Alert(String id, String title, String description, String severity, String time) {}

    public record ModerationQueueItem(String id, String type, String target, String status, String owner) {}

    public record QuickAction(String title, String text, String to) {}

    public record DashboardAuditItem(String id, String actor, String action, String date) {}

    public record DashboardPage(
            List<DashboardMetric> metrics,
            List<ServiceStatus> services,
            List<Alert> alerts,
            List<ModerationQueueItem> moderationQueue,
            List<QuickAction> quickActions,
            List<DashboardAuditItem> audit
    ) {}

    public record SimpleMetric(String icon, String value, String title, String meta, String tone) {}

    public record UserRow(
            String id,
            String initials,
            String name,
            String email,
            String role,
            String status,
            String risk,
            String tone,
            boolean active
    ) {}

    public record UserSignal(String title, String meta) {}

    public record SelectedUser(
            String initials,
            String name,
            String email,
            String role,
            String status,
            String riskTitle,
            List<UserSignal> signals,
            List<String> actions
    ) {}

    public record InfoCard(String title, String text, String action) {}

    public record RoleCard(String label, String value, String text, String action) {}

    public record UserCards(InfoCard audit, RoleCard roles, InfoCard moderation, InfoCard note) {}

    public record UsersPage(
            List<SimpleMetric> metrics,
            List<String> tabs,
            List<UserRow> users,
            SelectedUser selected,
            UserCards cards
    ) {}

    public record SiteMetric(String icon, String value, String title, String meta, String tone) {}

    public record GrowthChart(
            String title,
            String caption,
            List<String> labels,
            List<Integer> users,
            List<Integer> requests,
            String footer
    ) {}

    public record Conversion(String label, String value, String text) {}

    public record ProgressItem(String label, String value, int percent) {}

    public record FinanceSummary(String title, String value, String caption, String trend) {}

    public record GeoItem(String city, String value, int percent) {}

    public record SiteEvent(String time, String title, String meta, String tone) {}

    public record SiteNote(String title, String text, String action) {}

    public record SiteStatsPage(
            List<SiteMetric> metrics,
            List<String> tabs,
            GrowthChart growth,
            Conversion conversion,
            List<ProgressItem> quality,
            List<ProgressItem> funnel,
            FinanceSummary finance,
            List<GeoItem> geo,
            List<SiteEvent> events,
            SiteNote note
    ) {}

    public record SubscriptionMetric(String icon, String value, String label, String hint, String tone) {}

    public record SubscriptionRow(
            String id,
            String tutor,
            String plan,
            String status,
            String expiresAt,
            String payment,
            String tone
    ) {}

    public record PlanShare(String plan, String value, int percent, String tone) {}

    public record WebhookItem(String id, String event, String action, String tone) {}

    public record PlanSetting(String name, String price, String body) {}

    public record SubscriptionsPage(
            List<SubscriptionMetric> metrics,
            List<SubscriptionRow> subscriptions,
            List<PlanShare> revenueByPlan,
            List<WebhookItem> webhookQueue,
            List<PlanSetting> planSettings
    ) {}

    public record AuditMetric(String icon, String value, String label, String hint, String tone) {}

    public record AuditEvent(
            String id,
            String time,
            String actor,
            String role,
            String action,
            String target,
            String service,
            String severity,
            String status,
            String tone,
            boolean active
    ) {}

    public record AuditMeta(String label, String value) {}

    public record SelectedAudit(
            String id,
            String title,
            String actor,
            String description,
            List<AuditMeta> meta,
            List<String> payload
    ) {}

    public record AuditPolicy(String title, String text, int percent) {}

    public record AuditExport(String title, String text, String action) {}

    public record AuditPage(
            List<AuditMetric> metrics,
            List<String> tabs,
            List<AuditEvent> events,
            SelectedAudit selected,
            List<AuditPolicy> policies,
            List<AuditExport> exports
    ) {}

    public record SettingsMetric(String icon, String value, String label, String hint, String tone) {}

    public record SecuritySetting(String title, String description, boolean enabled) {}

    public record RoleSetting(String title, String description, String users) {}

    public record LimitSetting(String label, String value, String description) {}

    public record SettingsService(String name, String status, String description, String tone) {}

    public record PolicySetting(String title, String text, List<String> tags) {}

    public record SettingsPage(
            List<SettingsMetric> metrics,
            List<SecuritySetting> security,
            List<RoleSetting> roles,
            List<LimitSetting> limits,
            List<SettingsService> services,
            List<PolicySetting> policies
    ) {}

    public record ModeratorMetric(String icon, String value, String label, String hint, String trend, String tone) {}

    public record ModeratorProfileRow(
            String id,
            String itemType,
            String initials,
            String name,
            String subject,
            String type,
            String status,
            String summary,
            String score,
            String owner,
            String tone,
            boolean active
    ) {}

    public record ModeratorCheckItem(String label, boolean done) {}

    public record ModeratorSelectedProfile(
            String id,
            String status,
            String initials,
            String name,
            String subject,
            String experience,
            String risk,
            List<ModeratorCheckItem> checklist,
            String score,
            String note
    ) {}

    public record ModeratorPipeline(String label, String value, int percent) {}

    public record ModeratorDecision(String title, String body, List<String> tags) {}

    public record ModeratorRecentDecision(
            String id,
            String action,
            String targetType,
            String targetId,
            String decidedAt
    ) {}

    public record ModeratorDashboard(
            int pendingComplaints,
            int pendingReviews,
            int pendingProfiles,
            int totalPending,
            List<ModeratorRecentDecision> recentDecisions
    ) {}

    public record ModeratorReviewPage(
            List<ModeratorMetric> metrics,
            List<String> tabs,
            List<ModeratorProfileRow> profiles,
            ModeratorSelectedProfile selected,
            List<ModeratorPipeline> pipeline,
            List<String> rubric,
            ModeratorDecision decision
    ) {}
}
