package com.example.loanoriginationsystem.controller;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.dto.LoanApplicationRequest;
import com.example.loanoriginationsystem.dto.LoanApplicationResponse;
import com.example.loanoriginationsystem.dto.LoanStatusUpdateRequest;
import com.example.loanoriginationsystem.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> createLoanApplication(
        @Valid @RequestBody LoanApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(loanApplicationService.createLoanApplication(request));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<LoanApplicationResponse> getLoanApplication(
        @PathVariable String applicationId
    ) {
        return ResponseEntity.ok(loanApplicationService.getLoanApplication(applicationId));
    }

    @GetMapping
    public ResponseEntity<Page<LoanApplicationResponse>> getLoanApplications(
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam(value = "status", required = false) LoanApplication.ApplicationStatus status,
        @PageableDefault(
            size = 20,
            sort = "applicationDate",
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        return ResponseEntity.ok(loanApplicationService.getLoanApplications(userId, status, pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanApplicationResponse>> getLoanApplicationsByUser(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(loanApplicationService.getLoanApplicationsByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanApplicationResponse>> getLoanApplicationsByStatus(
        @PathVariable LoanApplication.ApplicationStatus status
    ) {
        return ResponseEntity.ok(loanApplicationService.getLoanApplicationsByStatus(status));
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<LoanApplicationResponse> updateLoanApplicationStatus(
        @PathVariable String applicationId,
        @Valid @RequestBody LoanStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
            loanApplicationService.updateLoanApplicationStatus(applicationId, request.getStatus())
        );
    }

    @PostMapping("/{applicationId}/process")
    public ResponseEntity<Void> processLoanApplication(@PathVariable String applicationId) {
        loanApplicationService.processLoanApplication(applicationId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> deleteLoanApplication(@PathVariable String applicationId) {
        loanApplicationService.deleteLoanApplication(applicationId);
        return ResponseEntity.noContent().build();
    }
}