package com.example.loanoriginationsystem.controller;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.dto.LoanApplicationResponse;
import com.example.loanoriginationsystem.service.LoanApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoanApplicationController.class)
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanApplicationService loanApplicationService;

    @Test
    void createLoanApplication_shouldReturnCreated() throws Exception {
        LoanApplicationResponse response = LoanApplicationResponse.builder()
            .applicationId("app-001")
            .userId(10L)
            .loanType(LoanApplication.LoanType.PERSONAL_LOAN)
            .amount(new BigDecimal("25000.00"))
            .termInMonths(36)
            .annualIncome(new BigDecimal("120000.00"))
            .existingMonthlyDebt(new BigDecimal("900.00"))
            .creditScore(740)
            .status(LoanApplication.ApplicationStatus.SUBMITTED)
            .applicationDate(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();

        when(loanApplicationService.createLoanApplication(any())).thenReturn(response);

        String requestBody = """
            {
              "userId": 10,
              "loanType": "PERSONAL_LOAN",
              "amount": 25000.00,
              "termInMonths": 36,
              "annualIncome": 120000.00,
              "existingMonthlyDebt": 900.00,
              "creditScore": 740
            }
            """;

        mockMvc.perform(post("/loan-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.applicationId").value("app-001"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void createLoanApplication_shouldReturnBadRequestForInvalidPayload() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/loan-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
