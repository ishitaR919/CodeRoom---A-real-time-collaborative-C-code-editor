package com.example.collabeditor.room;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<?> createRoom(Authentication authentication) {
        try {
            UUID userId = (UUID) authentication.getPrincipal();
            RoomDto.RoomResponse room = roomService.createRoom(userId);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserRooms(Authentication authentication) {
        try {
            UUID userId = (UUID) authentication.getPrincipal();
            List<RoomDto.RoomResponse> rooms = roomService.getUserRooms(userId);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody RoomDto.JoinRoomRequest request) {
        try {
            RoomDto.RoomResponse room = roomService.joinRoomByCode(request.getRoomCode());
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable UUID roomId) {
        try {
            RoomDto.RoomResponse room = roomService.getRoomById(roomId);
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
