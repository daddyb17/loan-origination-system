package com.example.loanoriginationsystem.mapper;

import com.example.loanoriginationsystem.domain.LoanApplication;
import com.example.loanoriginationsystem.dto.LoanApplicationRequest;
import com.example.loanoriginationsystem.dto.LoanApplicationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "applicationDate", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    LoanApplication toEntity(LoanApplicationRequest request);

    LoanApplicationResponse toResponse(LoanApplication loanApplication);
}
