package com.example.loanoriginationsystem.domain.check;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckResult {
    private CheckType checkType;
    private boolean passed;
    private String details;
    private String referenceId;
}
