package com.iitsm.domain.exception;

/**
 * OOP: Thrown when an individual notification channel delivery fails.
 * Logged and audited; never rolls back business transactions.
 */
public class NotificationFailureException extends IitsmException {

    private final String channelName;
    private final String recipient;

    public NotificationFailureException(String channelName, String recipient, String message, Throwable cause) {
        super(
                "ERR_NOTIFICATION_FAILED",
                String.format("Notification delivery failed on channel '%s' for recipient '%s': %s",
                        channelName, recipient, message),
                cause);
        this.channelName = channelName;
        this.recipient = recipient;
    }

    public NotificationFailureException(String channelName, String recipient, String message) {
        super(
                "ERR_NOTIFICATION_FAILED",
                String.format("Notification delivery failed on channel '%s' for recipient '%s': %s",
                        channelName, recipient, message));
        this.channelName = channelName;
        this.recipient = recipient;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getRecipient() {
        return recipient;
    }
}

