package com.example.synczone.models;

public class ModelChat {
    private String senderId, receiverId, message, image;
    private long timestamp;

    public ModelChat() { } // Required for Firebase

    public ModelChat(String senderId, String receiverId, String message, String image, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.image = image;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public String getImage() { return image; }
    public long getTimestamp() { return timestamp; }
}
