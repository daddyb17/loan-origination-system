package com.example.loanoriginationsystem.service.impl;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.domain.check.CheckResult;
import com.example.loanoriginationsystem.domain.check.CheckService;
import com.example.loanoriginationsystem.domain.decision.DecisionEngine;
import com.example.loanoriginationsystem.domain.decision.DecisionResult;
import com.example.loanoriginationsystem.domain.outbox.OutboxEvent;
import com.example.loanoriginationsystem.domain.outbox.OutboxEventPublisher;
import com.example.loanoriginationsystem.dto.LoanApplicationRequest;
import com.example.loanoriginationsystem.dto.LoanApplicationResponse;
import com.example.loanoriginationsystem.exception.BusinessRuleException;
import com.example.loanoriginationsystem.exception.LoanOriginationException;
import com.example.loanoriginationsystem.exception.ResourceNotFoundException;
import com.example.loanoriginationsystem.mapper.LoanApplicationMapper;
import com.example.loanoriginationsystem.repository.LoanApplicationRepository;
import com.example.loanoriginationsystem.service.LoanApplicationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationMapper loanApplicationMapper;
    private final DecisionEngine decisionEngine;
    private final CheckService checkService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LoanApplicationResponse createLoanApplication(LoanApplicationRequest request) {
        LoanApplication loanApplication = loanApplicationMapper.toEntity(request);
        loanApplication.setApplicationId(UUID.randomUUID().toString());
        loanApplication.setStatus(LoanApplication.ApplicationStatus.SUBMITTED);

        LoanApplication savedApplication = loanApplicationRepository.save(loanApplication);

        createAndPublishEvent(
            savedApplication.getApplicationId(),
            "LoanApplication",
            "LOAN_APPLICATION_CREATED",
            savedApplication
        );

        return loanApplicationMapper.toResponse(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getLoanApplication(String applicationId) {
        return loanApplicationMapper.toResponse(getExistingApplication(applicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getLoanApplications(
        Long userId,
        LoanApplication.ApplicationStatus status,
        Pageable pageable
    ) {
        if (userId != null && status != null) {
            return loanApplicationRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(userId, status, pageable)
                .map(loanApplicationMapper::toResponse);
        }
        if (userId != null) {
            return loanApplicationRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(loanApplicationMapper::toResponse);
        }
        if (status != null) {
            return loanApplicationRepository.findAllByStatusAndDeletedAtIsNull(status, pageable)
                .map(loanApplicationMapper::toResponse);
        }
        return loanApplicationRepository.findAllByDeletedAtIsNull(pageable).map(loanApplicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getLoanApplicationsByUser(Long userId) {
        return loanApplicationRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .map(loanApplicationMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getLoanApplicationsByStatus(LoanApplication.ApplicationStatus status) {
        return loanApplicationRepository.findByStatusAndDeletedAtIsNull(status).stream()
            .map(loanApplicationMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public LoanApplicationResponse updateLoanApplicationStatus(
        String applicationId,
        LoanApplication.ApplicationStatus newStatus
    ) {
        LoanApplication application = getExistingApplication(applicationId);
        LoanApplication.ApplicationStatus currentStatus = application.getStatus();
        if (currentStatus == newStatus) {
            return loanApplicationMapper.toResponse(application);
        }

        if (!application.canTransitionTo(newStatus)) {
            throw new BusinessRuleException(String.format(
                "Invalid status transition from %s to %s",
                currentStatus,
                newStatus
            ));
        }

        application.setStatus(newStatus);
        LoanApplication updatedApplication = loanApplicationRepository.save(application);

        createAndPublishEvent(
            applicationId,
            "LoanApplication",
            "LOAN_APPLICATION_STATUS_UPDATED",
            Map.of(
                "applicationId", applicationId,
                "previousStatus", currentStatus.name(),
                "newStatus", newStatus.name()
            )
        );

        return loanApplicationMapper.toResponse(updatedApplication);
    }

    @Override
    @Transactional
    public void processLoanApplication(String applicationId) {
        LoanApplication application = getExistingApplication(applicationId);

        if (application.getStatus().isFinalStatus()) {
            throw new BusinessRuleException("Cannot process an application in a final status");
        }

        if (application.getStatus() != LoanApplication.ApplicationStatus.SUBMITTED
            && application.getStatus() != LoanApplication.ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                "Application must be SUBMITTED or UNDER_REVIEW before processing"
            );
        }

        application.setStatus(LoanApplication.ApplicationStatus.UNDER_REVIEW);
        List<CheckResult> checkResults = checkService.performChecks(application);

        DecisionResult decision = decisionEngine.evaluate(application, checkResults);
        LoanApplication.ApplicationStatus targetStatus = mapDecisionToStatus(decision);

        if (!application.canTransitionTo(targetStatus)) {
            throw new BusinessRuleException(String.format(
                "Decision %s cannot be applied from status %s",
                decision,
                application.getStatus()
            ));
        }

        application.setStatus(targetStatus);
        LoanApplication updatedApplication = loanApplicationRepository.save(application);

        createAndPublishEvent(
            applicationId,
            "LoanApplication",
            "LOAN_DECISION_MADE",
            Map.of(
                "applicationId", applicationId,
                "decision", decision.name(),
                "status", updatedApplication.getStatus().name(),
                "checkResults", checkResults
            )
        );
    }

    @Override
    @Transactional
    public void deleteLoanApplication(String applicationId) {
        LoanApplication loanApplication = getExistingApplication(applicationId);

        if (!loanApplication.getStatus().isFinalStatus()) {
            throw new BusinessRuleException(
                "Only applications in final status can be deleted"
            );
        }

        loanApplication.setDeletedAt(LocalDateTime.now());
        loanApplicationRepository.save(loanApplication);

        createAndPublishEvent(
            applicationId,
            "LoanApplication",
            "LOAN_APPLICATION_DELETED",
            Map.of("applicationId", applicationId)
        );
    }

    private LoanApplication getExistingApplication(String applicationId) {
        return loanApplicationRepository.findByApplicationIdAndDeletedAtIsNull(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Loan application not found with id: " + applicationId
            ));
    }

    private LoanApplication.ApplicationStatus mapDecisionToStatus(DecisionResult decision) {
        return switch (decision) {
            case APPROVED -> LoanApplication.ApplicationStatus.APPROVED;
            case REJECTED -> LoanApplication.ApplicationStatus.REJECTED;
            case REFERRED, MANUAL_REVIEW -> LoanApplication.ApplicationStatus.UNDER_REVIEW;
        };
    }

    private void createAndPublishEvent(
        String aggregateId,
        String aggregateType,
        String eventType,
        Object payload
    ) {
        try {
            Map<String, Object> eventPayload = new LinkedHashMap<>();
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("occurredAt", LocalDateTime.now());
            eventPayload.put("aggregateId", aggregateId);
            eventPayload.put("aggregateType", aggregateType);
            eventPayload.put("eventType", eventType);
            eventPayload.put("payload", payload);

            OutboxEvent event = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(eventPayload))
                .build();

            outboxEventPublisher.publish(event);
        } catch (JsonProcessingException exception) {
            throw new LoanOriginationException("Failed to create outbox event", exception);
        }
    }
}
