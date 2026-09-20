package com.booking.event.notification.email;

public interface EmailClient {
    void send(EmailMessage message);
}
