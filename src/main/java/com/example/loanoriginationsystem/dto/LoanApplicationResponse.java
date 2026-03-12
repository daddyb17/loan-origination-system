package com.example.loanoriginationsystem.dto;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {
    private String applicationId;
    private Long userId;
    private LoanApplication.LoanType loanType;
    private BigDecimal amount;
    private Integer termInMonths;
    private BigDecimal annualIncome;
    private BigDecimal existingMonthlyDebt;
    private Integer creditScore;
    private LoanApplication.ApplicationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applicationDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;
}