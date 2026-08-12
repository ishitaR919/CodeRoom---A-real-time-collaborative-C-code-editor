package com.example.collabeditor.file;

import com.example.collabeditor.room.Room;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "code_files", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "filename"})
})
public class CodeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CodeFile() {
    }

    public CodeFile(UUID id, Room room, String filename, String content, OffsetDateTime updatedAt) {
        this.id = id;
        this.room = room;
        this.filename = filename;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    public void preSave() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
