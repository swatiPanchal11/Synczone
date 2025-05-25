package com.example.synczone.models;

public class Channel {
    private String name;
    private String type; // "text" or "voice"

    public Channel() { }

    public Channel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
}

