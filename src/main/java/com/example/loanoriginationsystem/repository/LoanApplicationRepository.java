package com.example.loanoriginationsystem.repository;

import com.example.loanoriginationsystem.domain.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationIdAndDeletedAtIsNull(String applicationId);

    List<LoanApplication> findByUserIdAndDeletedAtIsNull(Long userId);

    List<LoanApplication> findByStatusAndDeletedAtIsNull(LoanApplication.ApplicationStatus status);

    Page<LoanApplication> findAllByDeletedAtIsNull(Pageable pageable);

    Page<LoanApplication> findAllByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<LoanApplication> findAllByStatusAndDeletedAtIsNull(
        LoanApplication.ApplicationStatus status,
        Pageable pageable
    );

    Page<LoanApplication> findAllByUserIdAndStatusAndDeletedAtIsNull(
        Long userId,
        LoanApplication.ApplicationStatus status,
        Pageable pageable
    );

    boolean existsByApplicationId(String applicationId);
}
