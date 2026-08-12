package com.example.collabeditor.file;

import java.time.OffsetDateTime;
import java.util.UUID;

public class FileDto {

    public static class SaveFileRequest {
        private String content;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class FileResponse {
        private UUID id;
        private UUID roomId;
        private String filename;
        private String content;
        private OffsetDateTime updatedAt;

        public FileResponse() {}

        public FileResponse(UUID id, UUID roomId, String filename, String content, OffsetDateTime updatedAt) {
            this.id = id;
            this.roomId = roomId;
            this.filename = filename;
            this.content = content;
            this.updatedAt = updatedAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getRoomId() { return roomId; }
        public void setRoomId(UUID roomId) { this.roomId = roomId; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
