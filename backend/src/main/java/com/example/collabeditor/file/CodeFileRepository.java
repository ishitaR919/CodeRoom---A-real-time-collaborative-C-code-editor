package com.example.collabeditor.file;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CodeFileRepository extends JpaRepository<CodeFile, UUID> {
    Optional<CodeFile> findByRoomIdAndFilename(UUID roomId, String filename);
}
