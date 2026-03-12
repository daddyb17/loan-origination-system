package com.example.loanoriginationsystem.repository;

import com.example.loanoriginationsystem.domain.outbox.OutboxEvent;
import com.example.loanoriginationsystem.domain.outbox.OutboxEvent.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
        Collection<EventStatus> statuses,
        Integer attemptCount
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEvent event
        set event.status = :processingStatus,
            event.processingStartedAt = :processingStartedAt
        where event.id = :eventId
            and event.status in :claimableStatuses
            and event.attemptCount < :maxRetries
        """)
    int claimForProcessing(
        @Param("eventId") Long eventId,
        @Param("processingStatus") EventStatus processingStatus,
        @Param("processingStartedAt") LocalDateTime processingStartedAt,
        @Param("claimableStatuses") Collection<EventStatus> claimableStatuses,
        @Param("maxRetries") Integer maxRetries
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEvent event
        set event.status = :processedStatus,
            event.processedAt = :processedAt,
            event.lastError = null,
            event.processingStartedAt = null
        where event.id = :eventId
            and event.status = :processingStatus
        """)
    int markProcessed(
        @Param("eventId") Long eventId,
        @Param("processingStatus") EventStatus processingStatus,
        @Param("processedStatus") EventStatus processedStatus,
        @Param("processedAt") LocalDateTime processedAt
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEvent event
        set event.attemptCount = event.attemptCount + 1,
            event.status = case
                when event.attemptCount + 1 >= :maxRetries then :deadLetterStatus
                else :failedStatus
            end,
            event.lastError = :lastError,
            event.processingStartedAt = null
        where event.id = :eventId
            and event.status = :processingStatus
        """)
    int markFailedAttempt(
        @Param("eventId") Long eventId,
        @Param("processingStatus") EventStatus processingStatus,
        @Param("failedStatus") EventStatus failedStatus,
        @Param("deadLetterStatus") EventStatus deadLetterStatus,
        @Param("maxRetries") Integer maxRetries,
        @Param("lastError") String lastError
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OutboxEvent event
        set event.status = :recoveredStatus,
            event.lastError = :recoveryReason,
            event.processingStartedAt = null
        where event.status = :stuckStatus
            and event.processingStartedAt < :staleBefore
        """)
    int recoverStuckProcessingEvents(
        @Param("stuckStatus") EventStatus stuckStatus,
        @Param("recoveredStatus") EventStatus recoveredStatus,
        @Param("recoveryReason") String recoveryReason,
        @Param("staleBefore") LocalDateTime staleBefore
    );
}
