package com.otorael.BaseAuths.dto;

import com.otorael.BaseAuths.model.Auths;

import java.util.List;

public class GetAllUsers {

    private String notification;
    private String message;
    private String timestamp;
    private List<MultiUsersDto> userDetails;

    public GetAllUsers() {
    }

    public GetAllUsers(String notification, String message, String timestamp, List<MultiUsersDto> userDetails) {
        this.notification = notification;
        this.message = message;
        this.timestamp = timestamp;
        this.userDetails = userDetails;
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

    public List<MultiUsersDto> getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(List<MultiUsersDto> userDetails) {
        this.userDetails = userDetails;
    }
}
