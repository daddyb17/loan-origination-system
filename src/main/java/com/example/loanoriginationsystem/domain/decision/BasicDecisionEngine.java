package com.example.loanoriginationsystem.domain.decision;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.domain.check.CheckResult;
import com.example.loanoriginationsystem.domain.check.CheckType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class BasicDecisionEngine implements DecisionEngine {

    private static final int REFER_CREDIT_SCORE_THRESHOLD = 680;

    @Override
    public DecisionResult evaluate(LoanApplication application, List<CheckResult> checkResults) {
        boolean creditScoreCheckPassed = isPassed(checkResults, CheckType.CREDIT_SCORE);
        boolean incomeCheckPassed = isPassed(checkResults, CheckType.INCOME_VERIFICATION);
        boolean dtiCheckPassed = isPassed(checkResults, CheckType.DEBT_TO_INCOME);

        if (!creditScoreCheckPassed || !incomeCheckPassed) {
            return DecisionResult.REJECTED;
        }

        if (!dtiCheckPassed) {
            return DecisionResult.MANUAL_REVIEW;
        }

        BigDecimal monthlyIncome = application.getAnnualIncome()
            .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal dti = monthlyIncome.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ONE
            : application.getExistingMonthlyDebt()
                .divide(monthlyIncome, 6, RoundingMode.HALF_UP);

        boolean borderlineCredit = application.getCreditScore() < REFER_CREDIT_SCORE_THRESHOLD;
        boolean elevatedDti = dti.compareTo(new BigDecimal("0.35")) > 0;
        boolean highExposure = application.getAmount()
            .compareTo(application.getAnnualIncome().multiply(new BigDecimal("0.75"))) > 0;

        if (borderlineCredit || elevatedDti || highExposure) {
            return DecisionResult.REFERRED;
        }

        return DecisionResult.APPROVED;
    }

    private boolean isPassed(List<CheckResult> checkResults, CheckType checkType) {
        return checkResults.stream()
            .filter(result -> result.getCheckType() == checkType)
            .findFirst()
            .map(CheckResult::isPassed)
            .orElse(false);
    }
}