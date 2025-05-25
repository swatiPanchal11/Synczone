package com.example.synczone.models;

public class DirectChat {
    private String userId;
    private String username;
    private String profileImage;
    private String lastMessage;
    private long timestamp;

    // Constructor with all details
    public DirectChat(String userId, String username, String profileImage, String lastMessage, long timestamp) {
        this.userId = userId;
        this.username = username;
        this.profileImage = profileImage;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
