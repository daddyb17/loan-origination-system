package com.example.loanoriginationsystem.domain.check;

import com.example.loanoriginationsystem.domain.LoanApplication;
import java.util.List;

public interface CheckService {
    List<CheckResult> performChecks(LoanApplication application);
}
