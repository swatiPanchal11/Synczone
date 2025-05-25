package com.example.synczone.models;
public class ChatMessage {
    private String userId;
    private String senderName;
    private String message;
    private long timestamp;
    private String imageBase64;
    private boolean imageMessage;
    private String voiceBase64; // Base64-encoded voice message
    private boolean voiceMessage; // Flag for voice messages

    public ChatMessage() {
        // Default constructor required for Firebase
    }

    // Constructor for text messages
    public ChatMessage(String userId, String senderName, String message, long timestamp) {
        this.userId = userId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.imageMessage = false;
        this.voiceMessage = false;
    }

    // Constructor for image messages
    public ChatMessage(String userId, String senderName, String message, long timestamp, String imageBase64, boolean imageMessage) {
        this.userId = userId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.imageBase64 = imageBase64;
        this.imageMessage = imageMessage;
        this.voiceMessage = false;
    }

    // Constructor for voice messages
    public ChatMessage(String userId, String senderName, long timestamp, String voiceBase64, boolean voiceMessage) {
        this.userId = userId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.voiceBase64 = voiceBase64;
        this.voiceMessage = voiceMessage;
        this.imageMessage = false;
    }

    public String getUserId() { return userId; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public String getImageBase64() { return imageBase64; }
    public boolean isImageMessage() { return imageMessage; }
    public String getVoiceBase64() { return voiceBase64; }
    public boolean isVoiceMessage() { return voiceMessage; }
}
