package com.booking.event.outbox;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.booking.event.notification.queue.RedisQueueNames;
import com.booking.event.notification.queue.RedisQueueService;

@Component
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final RedisQueueService redisQueueService;
    private final int batchSize;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            RedisQueueService redisQueueService,
            @Value("${outbox.publisher.batch-size:25}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.redisQueueService = redisQueueService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEntity> outboxRecords =
                outboxRepository.findNextPublishableBatch(
                        List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                        PageRequest.of(0, batchSize));

        for (OutboxEntity outbox : outboxRecords) {
            publish(outbox);
        }
    }

    private void publish(OutboxEntity outbox) {
        outbox.setStatus(OutboxStatus.PROCESSING);
        outbox.setRetryCount(outbox.getRetryCount() + 1);

        try {
            redisQueueService.enqueueSerialized(
                    queueNameFor(outbox.getEventType()),
                    outbox.getPayload());

            outbox.setStatus(OutboxStatus.PUBLISHED);
            outbox.setLastError(null);
        } catch (Exception exception) {
            outbox.setStatus(OutboxStatus.FAILED);
            outbox.setLastError(exception.getMessage());
        }
    }

    private String queueNameFor(OutboxEventType eventType) {
        return switch (eventType) {
            case BOOKING_CONFIRMATION ->
                    RedisQueueNames.BOOKING_CONFIRMATION_QUEUE;
            case EVENT_UPDATE_NOTIFICATION ->
                    RedisQueueNames.EVENT_UPDATE_QUEUE;
        };
    }
}
