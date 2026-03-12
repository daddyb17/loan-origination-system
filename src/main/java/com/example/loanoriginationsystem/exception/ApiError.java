package com.example.loanoriginationsystem.exception;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ApiError {
    LocalDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
    String correlationId;
    List<String> details;
}