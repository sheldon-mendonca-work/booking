package com.booking.event.notification.email;

public record EmailMessage(
        String to,
        String subject,
        String text) {
}
