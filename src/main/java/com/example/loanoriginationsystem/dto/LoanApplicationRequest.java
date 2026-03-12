package com.example.loanoriginationsystem.dto;

import com.example.loanoriginationsystem.domain.LoanApplication;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be a positive number")
    private Long userId;

    @NotNull(message = "Loan type is required")
    private LoanApplication.LoanType loanType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1000.00")
    @Digits(integer = 17, fraction = 2, message = "Amount must have up to 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Term in months is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    @Max(value = 360, message = "Maximum term is 360 months")
    private Integer termInMonths;

    @NotNull(message = "Annual income is required")
    @DecimalMin(value = "1.00", message = "Annual income must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Annual income must have up to 2 decimal places")
    private BigDecimal annualIncome;

    @NotNull(message = "Existing monthly debt is required")
    @DecimalMin(value = "0.00", message = "Existing monthly debt cannot be negative")
    @Digits(integer = 17, fraction = 2, message = "Existing monthly debt must have up to 2 decimal places")
    private BigDecimal existingMonthlyDebt;

    @NotNull(message = "Credit score is required")
    @Min(value = 300, message = "Credit score must be between 300 and 850")
    @Max(value = 850, message = "Credit score must be between 300 and 850")
    private Integer creditScore;
}