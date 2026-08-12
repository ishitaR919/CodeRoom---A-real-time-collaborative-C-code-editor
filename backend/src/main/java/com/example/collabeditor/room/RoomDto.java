package com.example.collabeditor.room;

import com.example.collabeditor.auth.AuthDto;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RoomDto {

    public static class JoinRoomRequest {
        private String roomCode;

        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    }

    public static class RoomResponse {
        private UUID id;
        private String roomCode;
        private AuthDto.UserDto createdBy;
        private OffsetDateTime createdAt;

        public RoomResponse() {}

        public RoomResponse(UUID id, String roomCode, AuthDto.UserDto createdBy, OffsetDateTime createdAt) {
            this.id = id;
            this.roomCode = roomCode;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
        public AuthDto.UserDto getCreatedBy() { return createdBy; }
        public void setCreatedBy(AuthDto.UserDto createdBy) { this.createdBy = createdBy; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }
}
