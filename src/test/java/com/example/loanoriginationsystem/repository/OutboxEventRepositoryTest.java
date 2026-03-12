package com.example.loanoriginationsystem.repository;

import com.example.loanoriginationsystem.domain.outbox.OutboxEvent;
import com.example.loanoriginationsystem.domain.outbox.OutboxEvent.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void claimForProcessing_shouldClaimOnlyOnce() {
        OutboxEvent event = outboxEventRepository.save(buildEvent(EventStatus.PENDING, 0));

        int firstClaim = outboxEventRepository.claimForProcessing(
            event.getId(),
            EventStatus.PROCESSING,
            LocalDateTime.now(),
            Set.of(EventStatus.PENDING, EventStatus.FAILED),
            3
        );

        int secondClaim = outboxEventRepository.claimForProcessing(
            event.getId(),
            EventStatus.PROCESSING,
            LocalDateTime.now(),
            Set.of(EventStatus.PENDING, EventStatus.FAILED),
            3
        );

        OutboxEvent claimed = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(firstClaim).isEqualTo(1);
        assertThat(secondClaim).isEqualTo(0);
        assertThat(claimed.getStatus()).isEqualTo(EventStatus.PROCESSING);
        assertThat(claimed.getProcessingStartedAt()).isNotNull();
    }

    @Test
    void markFailedAttempt_shouldMoveToDeadLetterWhenMaxRetriesReached() {
        OutboxEvent event = outboxEventRepository.save(buildEvent(EventStatus.PROCESSING, 2));

        int updated = outboxEventRepository.markFailedAttempt(
            event.getId(),
            EventStatus.PROCESSING,
            EventStatus.FAILED,
            EventStatus.DEAD_LETTER,
            3,
            "publish failed"
        );

        OutboxEvent failed = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(failed.getAttemptCount()).isEqualTo(3);
        assertThat(failed.getStatus()).isEqualTo(EventStatus.DEAD_LETTER);
        assertThat(failed.getLastError()).isEqualTo("publish failed");
    }

    @Test
    void recoverStuckProcessingEvents_shouldReleaseExpiredClaims() {
        OutboxEvent event = outboxEventRepository.save(buildEvent(EventStatus.PROCESSING, 0));
        event.setProcessingStartedAt(LocalDateTime.now().minusMinutes(5));
        outboxEventRepository.save(event);

        int recovered = outboxEventRepository.recoverStuckProcessingEvents(
            EventStatus.PROCESSING,
            EventStatus.FAILED,
            "Recovered stale processing lock",
            LocalDateTime.now().minusMinutes(1)
        );

        OutboxEvent refreshed = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(recovered).isEqualTo(1);
        assertThat(refreshed.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(refreshed.getProcessingStartedAt()).isNull();
    }

    private OutboxEvent buildEvent(EventStatus status, int attemptCount) {
        return OutboxEvent.builder()
            .aggregateId("application-1")
            .aggregateType("LoanApplication")
            .eventType("LOAN_APPLICATION_CREATED")
            .payload("{\"applicationId\":\"application-1\"}")
            .status(status)
            .attemptCount(attemptCount)
            .build();
    }
}
