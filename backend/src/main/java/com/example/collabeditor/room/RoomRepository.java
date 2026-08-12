package com.example.collabeditor.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByRoomCode(String roomCode);
    List<Room> findByCreatedByIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByRoomCode(String roomCode);
}
