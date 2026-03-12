package com.example.loanoriginationsystem.domain.decision;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.domain.check.CheckResult;

import java.util.List;

public interface DecisionEngine {
    DecisionResult evaluate(LoanApplication application, List<CheckResult> checkResults);
}