package com.tutor.crmservice.service;

import com.tutor.crmservice.request.ApplicationStatus;

public record UpdateRequestStageRequest(ApplicationStatus status, String reason) {
}
