package com.otorael.BaseAuths.dto.responses;

public class CustomResponse {
    private String notification;
    private String message;
    private String timestamp;

    public CustomResponse() {
    }

    public CustomResponse(String notification, String message, String timestamp) {
        this.notification = notification;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
