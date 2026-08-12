package com.example.collabeditor.room;

import com.example.collabeditor.auth.AuthDto;
import com.example.collabeditor.file.CodeFile;
import com.example.collabeditor.file.CodeFileRepository;
import com.example.collabeditor.user.User;
import com.example.collabeditor.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String DEFAULT_CPP_CODE = "#include <iostream>\n\nint main() {\n    std::cout << \"Hello World\";\n    return 0;\n}\n";

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CodeFileRepository codeFileRepository;
    private final SecureRandom random = new SecureRandom();

    public RoomService(RoomRepository roomRepository, UserRepository userRepository, CodeFileRepository codeFileRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.codeFileRepository = codeFileRepository;
    }

    @Transactional
    public RoomDto.RoomResponse createRoom(UUID creatorUserId) {
        User user = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String roomCode = generateUniqueRoomCode();

        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setCreatedBy(user);
        room.setCreatedAt(OffsetDateTime.now());

        Room savedRoom = roomRepository.save(room);

        // Initialize main.cpp for the room
        CodeFile codeFile = new CodeFile();
        codeFile.setRoom(savedRoom);
        codeFile.setFilename("main.cpp");
        codeFile.setContent(DEFAULT_CPP_CODE);
        codeFile.setUpdatedAt(OffsetDateTime.now());
        codeFileRepository.save(codeFile);

        return toRoomResponse(savedRoom);
    }

    public RoomDto.RoomResponse joinRoomByCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }
        String cleanCode = roomCode.trim().toUpperCase();
        Room room = roomRepository.findByRoomCode(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + cleanCode));

        return toRoomResponse(room);
    }

    public RoomDto.RoomResponse getRoomById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return toRoomResponse(room);
    }

    public List<RoomDto.RoomResponse> getUserRooms(UUID userId) {
        return roomRepository.findByCreatedByIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    private RoomDto.RoomResponse toRoomResponse(Room room) {
        AuthDto.UserDto userDto = new AuthDto.UserDto(
                room.getCreatedBy().getId(),
                room.getCreatedBy().getName(),
                room.getCreatedBy().getEmail(),
                room.getCreatedBy().getCreatedAt()
        );
        return new RoomDto.RoomResponse(room.getId(), room.getRoomCode(), userDto, room.getCreatedAt());
    }
}
