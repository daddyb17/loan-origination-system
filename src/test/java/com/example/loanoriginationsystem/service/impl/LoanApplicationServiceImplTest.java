package com.example.loanoriginationsystem.service.impl;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.domain.check.CheckResult;
import com.example.loanoriginationsystem.domain.check.CheckType;
import com.example.loanoriginationsystem.domain.check.CheckService;
import com.example.loanoriginationsystem.domain.decision.DecisionEngine;
import com.example.loanoriginationsystem.domain.decision.DecisionResult;
import com.example.loanoriginationsystem.domain.outbox.OutboxEventPublisher;
import com.example.loanoriginationsystem.dto.LoanApplicationRequest;
import com.example.loanoriginationsystem.dto.LoanApplicationResponse;
import com.example.loanoriginationsystem.exception.BusinessRuleException;
import com.example.loanoriginationsystem.mapper.LoanApplicationMapper;
import com.example.loanoriginationsystem.repository.LoanApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceImplTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private DecisionEngine decisionEngine;

    @Mock
    private CheckService checkService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private LoanApplicationServiceImpl loanApplicationService;

    @Captor
    private ArgumentCaptor<LoanApplication> loanApplicationCaptor;

    private LoanApplicationRequest request;

    @BeforeEach
    void setUp() {
        loanApplicationService = new LoanApplicationServiceImpl(
            loanApplicationRepository,
            loanApplicationMapper,
            decisionEngine,
            checkService,
            outboxEventPublisher,
            new ObjectMapper().findAndRegisterModules()
        );

        request = new LoanApplicationRequest();
        request.setUserId(12L);
        request.setLoanType(LoanApplication.LoanType.PERSONAL_LOAN);
        request.setAmount(new BigDecimal("50000.00"));
        request.setTermInMonths(60);
        request.setAnnualIncome(new BigDecimal("150000.00"));
        request.setExistingMonthlyDebt(new BigDecimal("1500.00"));
        request.setCreditScore(720);
    }

    @Test
    void createLoanApplication_shouldPersistWithSubmittedStatusAndPublishEvent() {
        LoanApplication mapped = buildApplication(LoanApplication.ApplicationStatus.DRAFT);
        LoanApplication saved = buildApplication(LoanApplication.ApplicationStatus.SUBMITTED);
        saved.setApplicationId("app-001");

        LoanApplicationResponse response = LoanApplicationResponse.builder()
            .applicationId("app-001")
            .status(LoanApplication.ApplicationStatus.SUBMITTED)
            .build();

        when(loanApplicationMapper.toEntity(request)).thenReturn(mapped);
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(saved);
        when(loanApplicationMapper.toResponse(saved)).thenReturn(response);

        LoanApplicationResponse result = loanApplicationService.createLoanApplication(request);

        verify(loanApplicationRepository).save(loanApplicationCaptor.capture());
        assertThat(loanApplicationCaptor.getValue().getStatus())
            .isEqualTo(LoanApplication.ApplicationStatus.SUBMITTED);
        verify(outboxEventPublisher, times(1)).publish(any());
        assertThat(result.getApplicationId()).isEqualTo("app-001");
    }

    @Test
    void updateLoanApplicationStatus_shouldRejectInvalidTransition() {
        LoanApplication application = buildApplication(LoanApplication.ApplicationStatus.SUBMITTED);
        application.setApplicationId("app-002");

        when(loanApplicationRepository.findByApplicationIdAndDeletedAtIsNull("app-002"))
            .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanApplicationService.updateLoanApplicationStatus(
            "app-002",
            LoanApplication.ApplicationStatus.DISBURSED
        )).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateLoanApplicationStatus_shouldNotPersistOrPublishForNoOpTransition() {
        LoanApplication application = buildApplication(LoanApplication.ApplicationStatus.SUBMITTED);
        application.setApplicationId("app-002a");
        LoanApplicationResponse expected = LoanApplicationResponse.builder()
            .applicationId("app-002a")
            .status(LoanApplication.ApplicationStatus.SUBMITTED)
            .build();

        when(loanApplicationRepository.findByApplicationIdAndDeletedAtIsNull("app-002a"))
            .thenReturn(Optional.of(application));
        when(loanApplicationMapper.toResponse(application)).thenReturn(expected);

        LoanApplicationResponse result = loanApplicationService.updateLoanApplicationStatus(
            "app-002a",
            LoanApplication.ApplicationStatus.SUBMITTED
        );

        assertThat(result.getStatus()).isEqualTo(LoanApplication.ApplicationStatus.SUBMITTED);
        verify(loanApplicationRepository, never()).save(any(LoanApplication.class));
        verify(outboxEventPublisher, never()).publish(any());
    }

    @Test
    void processLoanApplication_shouldMoveToApprovedWhenDecisionApproved() {
        LoanApplication application = buildApplication(LoanApplication.ApplicationStatus.SUBMITTED);
        application.setApplicationId("app-003");

        List<CheckResult> checkResults = List.of(
            CheckResult.builder().checkType(CheckType.CREDIT_SCORE).passed(true).build(),
            CheckResult.builder().checkType(CheckType.INCOME_VERIFICATION).passed(true).build(),
            CheckResult.builder().checkType(CheckType.DEBT_TO_INCOME).passed(true).build()
        );

        when(loanApplicationRepository.findByApplicationIdAndDeletedAtIsNull("app-003"))
            .thenReturn(Optional.of(application));
        when(checkService.performChecks(application)).thenReturn(checkResults);
        when(decisionEngine.evaluate(application, checkResults)).thenReturn(DecisionResult.APPROVED);
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loanApplicationService.processLoanApplication("app-003");

        verify(loanApplicationRepository).save(loanApplicationCaptor.capture());
        assertThat(loanApplicationCaptor.getValue().getStatus())
            .isEqualTo(LoanApplication.ApplicationStatus.APPROVED);
        verify(outboxEventPublisher, times(1)).publish(any());
    }

    @Test
    void deleteLoanApplication_shouldRejectNonFinalStatus() {
        LoanApplication application = buildApplication(LoanApplication.ApplicationStatus.UNDER_REVIEW);
        application.setApplicationId("app-004");

        when(loanApplicationRepository.findByApplicationIdAndDeletedAtIsNull("app-004"))
            .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanApplicationService.deleteLoanApplication("app-004"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("final status");
    }

    @Test
    void deleteLoanApplication_shouldSoftDeleteForFinalStatus() {
        LoanApplication application = buildApplication(LoanApplication.ApplicationStatus.REJECTED);
        application.setApplicationId("app-005");

        when(loanApplicationRepository.findByApplicationIdAndDeletedAtIsNull("app-005"))
            .thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        loanApplicationService.deleteLoanApplication("app-005");

        verify(loanApplicationRepository, times(1)).save(loanApplicationCaptor.capture());
        assertThat(loanApplicationCaptor.getValue().getDeletedAt()).isNotNull();
        verify(outboxEventPublisher, times(1)).publish(any());
    }

    private LoanApplication buildApplication(LoanApplication.ApplicationStatus status) {
        return LoanApplication.builder()
            .id(1L)
            .applicationId("placeholder")
            .userId(request.getUserId())
            .loanType(request.getLoanType())
            .amount(request.getAmount())
            .termInMonths(request.getTermInMonths())
            .annualIncome(request.getAnnualIncome())
            .existingMonthlyDebt(request.getExistingMonthlyDebt())
            .creditScore(request.getCreditScore())
            .status(status)
            .applicationDate(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .version(0L)
            .build();
    }
}


