package com.example.loanoriginationsystem.domain.outbox;

public interface OutboxEventPublisher {
    void publish(OutboxEvent event);
}
