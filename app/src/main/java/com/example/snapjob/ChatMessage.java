package com.example.snapjob;

import java.util.Date;

public class ChatMessage {
    private String sender;
    private String message;
    private Date timestamp;
    private String userEmail;
    private boolean fromSupport;

    public ChatMessage() {}

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public boolean isFromSupport() { return fromSupport; }
    public void setFromSupport(boolean fromSupport) { this.fromSupport = fromSupport; }
}
