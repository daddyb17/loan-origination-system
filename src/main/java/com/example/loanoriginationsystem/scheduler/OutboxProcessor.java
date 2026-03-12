package com.example.loanoriginationsystem.scheduler;

import com.example.loanoriginationsystem.domain.outbox.OutboxEvent;
import com.example.loanoriginationsystem.domain.outbox.OutboxEvent.EventStatus;
import com.example.loanoriginationsystem.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    @Value("${outbox.max-retries:3}")
    private int maxRetries;

    @Value("${outbox.publish-timeout-seconds:5}")
    private long publishTimeoutSeconds;

    @Value("${outbox.processing-timeout-seconds:60}")
    private long processingTimeoutSeconds;

    @Scheduled(fixedDelayString = "${outbox.processor-delay-ms:5000}")
    public void processOutboxEvents() {
        recoverStaleProcessingEvents();

        List<OutboxEvent> events = outboxEventRepository
            .findTop100ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                Set.of(EventStatus.PENDING, EventStatus.FAILED),
                maxRetries
            );

        for (OutboxEvent event : events) {
            if (!claimEvent(event.getId())) {
                continue;
            }

            try {
                kafkaTemplate.send(loanEventsTopic, event.getAggregateId(), event.getPayload())
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);

                outboxEventRepository.markProcessed(
                    event.getId(),
                    EventStatus.PROCESSING,
                    EventStatus.PROCESSED,
                    LocalDateTime.now()
                );
                log.debug("Published outbox event {}", event.getId());
            } catch (Exception exception) {
                markFailure(event.getId(), exception);
            }
        }
    }

    private boolean claimEvent(Long eventId) {
        int claimed = outboxEventRepository.claimForProcessing(
            eventId,
            EventStatus.PROCESSING,
            LocalDateTime.now(),
            Set.of(EventStatus.PENDING, EventStatus.FAILED),
            maxRetries
        );
        return claimed == 1;
    }

    private void recoverStaleProcessingEvents() {
        LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        int recovered = outboxEventRepository.recoverStuckProcessingEvents(
            EventStatus.PROCESSING,
            EventStatus.FAILED,
            "Recovered stale processing lock",
            staleBefore
        );
        if (recovered > 0) {
            log.warn("Recovered {} stale outbox events stuck in PROCESSING", recovered);
        }
    }

    private void markFailure(Long eventId, Exception exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        String sanitizedMessage = message.length() > ERROR_MESSAGE_MAX_LENGTH
            ? message.substring(0, ERROR_MESSAGE_MAX_LENGTH)
            : message;
        outboxEventRepository.markFailedAttempt(
            eventId,
            EventStatus.PROCESSING,
            EventStatus.FAILED,
            EventStatus.DEAD_LETTER,
            maxRetries,
            sanitizedMessage
        );
        log.error("Failed to publish outbox event {}", eventId, exception);
    }
}
