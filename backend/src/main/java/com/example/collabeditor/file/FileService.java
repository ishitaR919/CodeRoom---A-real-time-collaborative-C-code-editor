package com.example.collabeditor.file;

import com.example.collabeditor.room.Room;
import com.example.collabeditor.room.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class FileService {

    private static final String DEFAULT_FILENAME = "main.cpp";
    private static final String DEFAULT_CPP_CODE = "#include <iostream>\n\nint main() {\n    std::cout << \"Hello World\";\n    return 0;\n}\n";

    private final CodeFileRepository codeFileRepository;
    private final RoomRepository roomRepository;

    public FileService(CodeFileRepository codeFileRepository, RoomRepository roomRepository) {
        this.codeFileRepository = codeFileRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public FileDto.FileResponse getFileForRoom(UUID roomId) {
        CodeFile file = codeFileRepository.findByRoomIdAndFilename(roomId, DEFAULT_FILENAME)
                .orElseGet(() -> {
                    Room room = roomRepository.findById(roomId)
                            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                    CodeFile newFile = new CodeFile();
                    newFile.setRoom(room);
                    newFile.setFilename(DEFAULT_FILENAME);
                    newFile.setContent(DEFAULT_CPP_CODE);
                    newFile.setUpdatedAt(OffsetDateTime.now());
                    return codeFileRepository.save(newFile);
                });

        return new FileDto.FileResponse(file.getId(), file.getRoom().getId(), file.getFilename(), file.getContent(), file.getUpdatedAt());
    }

    @Transactional
    public FileDto.FileResponse saveFileForRoom(UUID roomId, String content) {
        if (content == null) {
            content = "";
        }
        final String finalContent = content;

        CodeFile file = codeFileRepository.findByRoomIdAndFilename(roomId, DEFAULT_FILENAME)
                .orElseGet(() -> {
                    Room room = roomRepository.findById(roomId)
                            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                    CodeFile newFile = new CodeFile();
                    newFile.setRoom(room);
                    newFile.setFilename(DEFAULT_FILENAME);
                    newFile.setContent(finalContent);
                    newFile.setUpdatedAt(OffsetDateTime.now());
                    return newFile;
                });

        file.setContent(content);
        file.setUpdatedAt(OffsetDateTime.now());
        CodeFile savedFile = codeFileRepository.save(file);

        return new FileDto.FileResponse(savedFile.getId(), savedFile.getRoom().getId(), savedFile.getFilename(), savedFile.getContent(), savedFile.getUpdatedAt());
    }
}
