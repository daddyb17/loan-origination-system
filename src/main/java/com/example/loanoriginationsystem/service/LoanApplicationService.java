package com.example.loanoriginationsystem.service;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.dto.LoanApplicationRequest;
import com.example.loanoriginationsystem.dto.LoanApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanApplicationService {

    LoanApplicationResponse createLoanApplication(LoanApplicationRequest request);

    LoanApplicationResponse getLoanApplication(String applicationId);

    Page<LoanApplicationResponse> getLoanApplications(
        Long userId,
        LoanApplication.ApplicationStatus status,
        Pageable pageable
    );

    List<LoanApplicationResponse> getLoanApplicationsByUser(Long userId);

    List<LoanApplicationResponse> getLoanApplicationsByStatus(LoanApplication.ApplicationStatus status);

    LoanApplicationResponse updateLoanApplicationStatus(
        String applicationId,
        LoanApplication.ApplicationStatus newStatus
    );

    void processLoanApplication(String applicationId);

    void deleteLoanApplication(String applicationId);
}