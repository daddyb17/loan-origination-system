package com.example.loanoriginationsystem.scheduler;

import com.example.loanoriginationsystem.domain.outbox.OutboxEvent;
import com.example.loanoriginationsystem.domain.outbox.OutboxEvent.EventStatus;
import com.example.loanoriginationsystem.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        outboxProcessor = new OutboxProcessor(outboxEventRepository, kafkaTemplate);
        ReflectionTestUtils.setField(outboxProcessor, "loanEventsTopic", "loan-events");
        ReflectionTestUtils.setField(outboxProcessor, "maxRetries", 3);
        ReflectionTestUtils.setField(outboxProcessor, "publishTimeoutSeconds", 5L);
        ReflectionTestUtils.setField(outboxProcessor, "processingTimeoutSeconds", 60L);
    }

    @Test
    void processOutboxEvents_shouldPublishAndMarkProcessedWhenClaimed() {
        OutboxEvent event = OutboxEvent.builder()
            .id(1L)
            .aggregateId("app-1")
            .payload("{\"key\":\"value\"}")
            .status(EventStatus.PENDING)
            .attemptCount(0)
            .build();

        when(outboxEventRepository.recoverStuckProcessingEvents(any(), any(), any(), any())).thenReturn(0);
        when(outboxEventRepository.findTop100ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Set.of(EventStatus.PENDING, EventStatus.FAILED),
            3
        )).thenReturn(List.of(event));
        when(outboxEventRepository.claimForProcessing(
            eq(1L),
            eq(EventStatus.PROCESSING),
            any(),
            eq(Set.of(EventStatus.PENDING, EventStatus.FAILED)),
            eq(3)
        )).thenReturn(1);
        when(kafkaTemplate.send("loan-events", "app-1", "{\"key\":\"value\"}"))
            .thenReturn(CompletableFuture.completedFuture(null));

        outboxProcessor.processOutboxEvents();

        verify(kafkaTemplate).send("loan-events", "app-1", "{\"key\":\"value\"}");
        verify(outboxEventRepository).markProcessed(
            eq(1L),
            eq(EventStatus.PROCESSING),
            eq(EventStatus.PROCESSED),
            any()
        );
        verify(outboxEventRepository, never()).markFailedAttempt(any(), any(), any(), any(), any(), any());
    }

    @Test
    void processOutboxEvents_shouldSkipPublishWhenClaimFails() {
        OutboxEvent event = OutboxEvent.builder()
            .id(2L)
            .aggregateId("app-2")
            .payload("{\"foo\":\"bar\"}")
            .status(EventStatus.PENDING)
            .attemptCount(0)
            .build();

        when(outboxEventRepository.recoverStuckProcessingEvents(any(), any(), any(), any())).thenReturn(0);
        when(outboxEventRepository.findTop100ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Set.of(EventStatus.PENDING, EventStatus.FAILED),
            3
        )).thenReturn(List.of(event));
        when(outboxEventRepository.claimForProcessing(
            eq(2L),
            eq(EventStatus.PROCESSING),
            any(),
            eq(Set.of(EventStatus.PENDING, EventStatus.FAILED)),
            eq(3)
        )).thenReturn(0);

        outboxProcessor.processOutboxEvents();

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(outboxEventRepository, never()).markProcessed(any(), any(), any(), any());
        verify(outboxEventRepository, never()).markFailedAttempt(any(), any(), any(), any(), any(), any());
    }
}
