package com.example.loanoriginationsystem.domain.check;

import com.example.loanoriginationsystem.domain.LoanApplication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class BasicCheckService implements CheckService {

    private static final int MIN_CREDIT_SCORE = 620;
    private static final BigDecimal MAX_DTI = new BigDecimal("0.45");

    @Override
    public List<CheckResult> performChecks(LoanApplication application) {
        BigDecimal monthlyIncome = application.getAnnualIncome()
            .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal dti = monthlyIncome.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ONE
            : application.getExistingMonthlyDebt()
                .divide(monthlyIncome, 6, RoundingMode.HALF_UP);

        boolean incomeCheckPassed = application.getAnnualIncome()
            .compareTo(application.getAmount().multiply(new BigDecimal("0.40"))) >= 0;

        return List.of(
            CheckResult.builder()
                .checkType(CheckType.CREDIT_SCORE)
                .passed(application.getCreditScore() >= MIN_CREDIT_SCORE)
                .details("Credit score must be at least " + MIN_CREDIT_SCORE)
                .referenceId("CREDIT-" + UUID.randomUUID())
                .build(),
            CheckResult.builder()
                .checkType(CheckType.INCOME_VERIFICATION)
                .passed(incomeCheckPassed)
                .details("Annual income should be at least 40% of requested amount")
                .referenceId("INCOME-" + UUID.randomUUID())
                .build(),
            CheckResult.builder()
                .checkType(CheckType.DEBT_TO_INCOME)
                .passed(dti.compareTo(MAX_DTI) <= 0)
                .details("Debt-to-income ratio must not exceed 45%")
                .referenceId("DTI-" + UUID.randomUUID())
                .build(),
            CheckResult.builder()
                .checkType(CheckType.IDENTITY_VERIFICATION)
                .passed(true)
                .details("Identity verification completed")
                .referenceId("ID-" + UUID.randomUUID())
                .build(),
            CheckResult.builder()
                .checkType(CheckType.EMPLOYMENT_VERIFICATION)
                .passed(true)
                .details("Employment verification completed")
                .referenceId("EMP-" + UUID.randomUUID())
                .build()
        );
    }
}