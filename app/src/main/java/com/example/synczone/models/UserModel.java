package com.example.synczone.models;

public class UserModel {
    private String userId;
    private String username;
    private String email; // 🔹 Added email field
    private String profileImage; // 🔹 Added profileImage field
    private String role;

    public UserModel() {
        // Required empty constructor for Firebase
    }

    public UserModel(String userId, String username, String email, String profileImage, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImage = profileImage;
        this.role = role;
    }

    // 🔹 Getter & Setter for userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // 🔹 Getter & Setter for username
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // 🔹 Getter & Setter for email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // 🔹 Getter & Setter for profileImage
    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    // 🔹 Getter & Setter for role
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
