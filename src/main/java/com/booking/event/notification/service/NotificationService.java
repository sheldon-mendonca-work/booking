package com.booking.event.notification.service;

import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.notification.job.EventUpdateNotificationJob;

public interface NotificationService {
    void processBookingConfirmation(BookingConfirmationJob job);

    void processEventUpdateNotification(EventUpdateNotificationJob job);
}
