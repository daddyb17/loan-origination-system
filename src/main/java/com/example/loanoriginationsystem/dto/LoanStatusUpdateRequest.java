package com.example.loanoriginationsystem.dto;

import com.example.loanoriginationsystem.domain.LoanApplication;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private LoanApplication.ApplicationStatus status;
}