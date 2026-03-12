package com.example.loanoriginationsystem.controller;

import com.example.loanoriginationsystem.exception.BusinessRuleException;
import com.example.loanoriginationsystem.exception.ResourceNotFoundException;
import com.example.loanoriginationsystem.service.LoanApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoanApplicationController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanApplicationService loanApplicationService;

    @Test
    void shouldReturnValidationErrorContract() throws Exception {
        mockMvc.perform(post("/loan-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.path").value("/loan-applications"))
            .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturnBusinessRuleErrorContract() throws Exception {
        doThrow(new BusinessRuleException("Application must be SUBMITTED before processing"))
            .when(loanApplicationService)
            .processLoanApplication("app-100");

        mockMvc.perform(post("/loan-applications/app-100/process"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
            .andExpect(jsonPath("$.message").value("Application must be SUBMITTED before processing"))
            .andExpect(jsonPath("$.path").value("/loan-applications/app-100/process"));
    }

    @Test
    void shouldReturnNotFoundErrorContract() throws Exception {
        when(loanApplicationService.getLoanApplication("missing-id"))
            .thenThrow(new ResourceNotFoundException("Loan application not found with id: missing-id"));

        mockMvc.perform(get("/loan-applications/missing-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Loan application not found with id: missing-id"))
            .andExpect(jsonPath("$.path").value("/loan-applications/missing-id"));
    }

    @Test
    void shouldReturnInternalServerErrorContract() throws Exception {
        when(loanApplicationService.getLoanApplication("boom"))
            .thenThrow(new RuntimeException("kaboom"));

        mockMvc.perform(get("/loan-applications/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.details[0]").value("RuntimeException"));
    }
}
