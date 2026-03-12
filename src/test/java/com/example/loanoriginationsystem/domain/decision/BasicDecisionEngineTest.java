package com.example.loanoriginationsystem.domain.decision;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.domain.check.CheckResult;
import com.example.loanoriginationsystem.domain.check.CheckType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicDecisionEngineTest {

    private final BasicDecisionEngine decisionEngine = new BasicDecisionEngine();

    @Test
    void evaluate_shouldApproveForStrongProfile() {
        LoanApplication application = buildApplication(720, "200000.00", "2000.00", "50000.00");

        DecisionResult result = decisionEngine.evaluate(application, passingChecks());

        assertThat(result).isEqualTo(DecisionResult.APPROVED);
    }

    @Test
    void evaluate_shouldRejectWhenCreditOrIncomeChecksFail() {
        LoanApplication application = buildApplication(580, "70000.00", "1000.00", "50000.00");

        List<CheckResult> failedCreditChecks = List.of(
            CheckResult.builder().checkType(CheckType.CREDIT_SCORE).passed(false).build(),
            CheckResult.builder().checkType(CheckType.INCOME_VERIFICATION).passed(true).build(),
            CheckResult.builder().checkType(CheckType.DEBT_TO_INCOME).passed(true).build()
        );

        DecisionResult result = decisionEngine.evaluate(application, failedCreditChecks);

        assertThat(result).isEqualTo(DecisionResult.REJECTED);
    }

    @Test
    void evaluate_shouldReferForBorderlineRiskProfile() {
        LoanApplication application = buildApplication(670, "150000.00", "4500.00", "90000.00");

        DecisionResult result = decisionEngine.evaluate(application, passingChecks());

        assertThat(result).isEqualTo(DecisionResult.REFERRED);
    }

    private LoanApplication buildApplication(
        int creditScore,
        String annualIncome,
        String monthlyDebt,
        String amount
    ) {
        return LoanApplication.builder()
            .id(1L)
            .applicationId("app-1")
            .userId(100L)
            .loanType(LoanApplication.LoanType.PERSONAL_LOAN)
            .amount(new BigDecimal(amount))
            .termInMonths(48)
            .annualIncome(new BigDecimal(annualIncome))
            .existingMonthlyDebt(new BigDecimal(monthlyDebt))
            .creditScore(creditScore)
            .status(LoanApplication.ApplicationStatus.UNDER_REVIEW)
            .applicationDate(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .version(0L)
            .build();
    }

    private List<CheckResult> passingChecks() {
        return List.of(
            CheckResult.builder().checkType(CheckType.CREDIT_SCORE).passed(true).build(),
            CheckResult.builder().checkType(CheckType.INCOME_VERIFICATION).passed(true).build(),
            CheckResult.builder().checkType(CheckType.DEBT_TO_INCOME).passed(true).build()
        );
    }
}