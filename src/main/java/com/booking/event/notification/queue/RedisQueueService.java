package com.booking.event.notification.queue;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisQueueService {
    private final StringRedisTemplate redisTemplate;
    private final JobPayloadCodec jobPayloadCodec;

    public RedisQueueService(
            StringRedisTemplate redisTemplate,
            JobPayloadCodec jobPayloadCodec) {
        this.redisTemplate = redisTemplate;
        this.jobPayloadCodec = jobPayloadCodec;
    }

    public void enqueue(String queueName, Object payload) {
        enqueueSerialized(queueName, jobPayloadCodec.serialize(payload));
    }

    public void enqueueSerialized(String queueName, String payload) {
        redisTemplate.opsForList().rightPush(queueName, payload);
    }

    public <T> Optional<T> dequeue(
            String queueName,
            Class<T> payloadType,
            Duration timeout) {
        String payload = redisTemplate.opsForList().leftPop(queueName, timeout);

        if (payload == null) {
            return Optional.empty();
        }

        return Optional.of(jobPayloadCodec.deserialize(payload, payloadType));
    }
}
