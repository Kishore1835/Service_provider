package com.example.snapjob;

import java.util.Date;

public class NotificationModel {
    private String title;
    private String message;
    private String type;
    private Date timestamp;
    private String userEmail;

    public NotificationModel() {} // Required for Firestore

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
