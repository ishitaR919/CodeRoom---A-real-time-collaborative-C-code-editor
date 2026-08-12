package com.example.collabeditor.file;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ResponseEntity<?> getFile(@PathVariable UUID roomId) {
        try {
            FileDto.FileResponse file = fileService.getFileForRoom(roomId);
            return ResponseEntity.ok(file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> saveFile(@PathVariable UUID roomId, @RequestBody FileDto.SaveFileRequest request) {
        try {
            FileDto.FileResponse file = fileService.saveFileForRoom(roomId, request.getContent());
            return ResponseEntity.ok(file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
