package com.example.loanoriginationsystem.domain.outbox;

import com.example.loanoriginationsystem.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaOutboxEventPublisher implements OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public void publish(OutboxEvent event) {
        outboxEventRepository.save(event);
    }
}