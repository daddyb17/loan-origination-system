package com.example.loanoriginationsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(
    name = "loan_applications",
    indexes = {
        @Index(name = "idx_loan_applications_user_id", columnList = "user_id"),
        @Index(name = "idx_loan_applications_status", columnList = "status"),
        @Index(name = "idx_loan_applications_created_at", columnList = "application_date")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uk_loan_applications_application_id",
        columnNames = "application_id"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        ApplicationStatus.DRAFT, EnumSet.of(ApplicationStatus.PENDING, ApplicationStatus.CANCELLED),
        ApplicationStatus.PENDING, EnumSet.of(ApplicationStatus.SUBMITTED, ApplicationStatus.CANCELLED),
        ApplicationStatus.SUBMITTED, EnumSet.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CANCELLED),
        ApplicationStatus.UNDER_REVIEW, EnumSet.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, ApplicationStatus.CANCELLED),
        ApplicationStatus.APPROVED, EnumSet.of(ApplicationStatus.DISBURSED, ApplicationStatus.CANCELLED),
        ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class),
        ApplicationStatus.DISBURSED, EnumSet.noneOf(ApplicationStatus.class),
        ApplicationStatus.CANCELLED, EnumSet.noneOf(ApplicationStatus.class)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, updatable = false, length = 36)
    private String applicationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanType loanType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "term_in_months", nullable = false)
    private Integer termInMonths;

    @Column(name = "annual_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "existing_monthly_debt", nullable = false, precision = 19, scale = 2)
    private BigDecimal existingMonthlyDebt;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    public enum LoanType {
        PERSONAL_LOAN,
        HOME_LOAN,
        CAR_LOAN,
        EDUCATION_LOAN
    }

    public enum ApplicationStatus {
        DRAFT,
        PENDING,
        SUBMITTED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        DISBURSED,
        CANCELLED;

        public boolean isFinalStatus() {
            return this == REJECTED || this == DISBURSED || this == CANCELLED;
        }
    }

    public boolean canTransitionTo(ApplicationStatus newStatus) {
        if (status == null || newStatus == null) {
            return false;
        }
        if (status == newStatus) {
            return true;
        }
        return ALLOWED_STATUS_TRANSITIONS.getOrDefault(status, Set.of()).contains(newStatus);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.applicationDate = now;
        this.lastUpdated = now;
        if (this.status == null) {
            this.status = ApplicationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
