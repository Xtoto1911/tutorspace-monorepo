package com.tutor.crmservice.view;

import java.util.List;

public record ClientMessagesPageDto(
        List<MetricDto> metrics,
        List<String> filters,
        List<ConversationDto> conversations,
        DialogHeaderDto activeDialog,
        List<MessageDto> messages,
        List<DetailDto> lessonDetails,
        List<String> quickActions,
        List<AttachmentDto> attachments
) {
    public record MetricDto(String value, String label, String tone) {
    }

    public record ConversationDto(String id,
                                  String name,
                                  String initials,
                                  String subject,
                                  String preview,
                                  String time,
                                  int unread,
                                  boolean active) {
    }

    public record DialogHeaderDto(String threadId, String name, String initials, String subtitle, String tutorSlug, String applicationId) {
    }

    public record MessageDto(String id, String author, String time, String text, boolean own) {
    }

    public record DetailDto(String label, String value) {
    }

    public record AttachmentDto(String name, String meta) {
    }
}
