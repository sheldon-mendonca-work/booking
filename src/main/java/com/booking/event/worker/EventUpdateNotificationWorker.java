package com.booking.event.worker;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.booking.event.notification.job.EventUpdateNotificationJob;
import com.booking.event.notification.queue.RedisQueueNames;
import com.booking.event.notification.queue.RedisQueueService;
import com.booking.event.notification.service.NotificationService;

@Component
public class EventUpdateNotificationWorker implements SmartLifecycle {
  private static final Logger logger =
      LoggerFactory.getLogger(EventUpdateNotificationWorker.class);
  private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

  private final RedisQueueService redisQueueService;
  private final NotificationService notificationService;
  private final ExecutorService executorService =
      Executors.newSingleThreadExecutor();

  private volatile boolean running;

  public EventUpdateNotificationWorker(
      RedisQueueService redisQueueService,
      NotificationService notificationService) {
    this.redisQueueService = redisQueueService;
    this.notificationService = notificationService;
  }

  @Override
  public void start() {
    if (running) {
      return;
    }

    running = true;
    executorService.submit(this::consume);
  }

  private void consume() {
    while (running && !Thread.currentThread().isInterrupted()) {
      try {
        redisQueueService
            .dequeue(
                RedisQueueNames.EVENT_UPDATE_QUEUE,
                EventUpdateNotificationJob.class,
                BLOCK_TIMEOUT)
            .ifPresent(notificationService::processEventUpdateNotification);
      } catch (Exception exception) {
        logger.error("Failed to process event update notification job", exception);
      }
    }
  }

  @Override
  public void stop() {
    running = false;
    executorService.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
