package com.tutor.crmservice.view;

import java.util.List;

public record TutorMessagesPageDto(
        List<MetricDto> metrics,
        List<ConversationDto> conversations,
        ActiveDialogDto activeDialog,
        List<MessageDto> messages,
        List<DetailDto> details,
        List<String> quickActions,
        List<MaterialDto> materials
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

    public record ActiveDialogDto(String threadId, String initials, String name, String subtitle, String requestId, String studentId) {
    }

    public record MessageDto(String id, String author, String time, String text, boolean own) {
    }

    public record DetailDto(String label, String value) {
    }

    public record MaterialDto(String name, String meta) {
    }
}
