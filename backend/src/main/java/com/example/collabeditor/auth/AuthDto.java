package com.example.collabeditor.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuthDto {

    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UserDto {
        private UUID id;
        private String name;
        private String email;
        private OffsetDateTime createdAt;

        public UserDto() {}

        public UserDto(UUID id, String name, String email, OffsetDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class AuthResponse {
        private UserDto user;
        private String token;

        public AuthResponse() {}

        public AuthResponse(UserDto user, String token) {
            this.user = user;
            this.token = token;
        }

        public UserDto getUser() { return user; }
        public void setUser(UserDto user) { this.user = user; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
