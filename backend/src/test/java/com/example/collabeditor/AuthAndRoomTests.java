package com.example.collabeditor;

import com.example.collabeditor.auth.AuthDto;
import com.example.collabeditor.auth.AuthService;
import com.example.collabeditor.room.RoomDto;
import com.example.collabeditor.room.RoomService;
import com.example.collabeditor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthAndRoomTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterAndLogin() {
        AuthDto.RegisterRequest regReq = new AuthDto.RegisterRequest();
        regReq.setName("Test User");
        regReq.setEmail("test@example.com");
        regReq.setPassword("secret123");

        AuthDto.AuthResponse regRes = authService.register(regReq);
        assertNotNull(regRes.getToken());
        assertEquals("Test User", regRes.getUser().getName());

        AuthDto.LoginRequest loginReq = new AuthDto.LoginRequest();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("secret123");

        AuthDto.AuthResponse loginRes = authService.login(loginReq);
        assertNotNull(loginRes.getToken());
        assertEquals("test@example.com", loginRes.getUser().getEmail());
    }

    @Test
    void testDuplicateEmail() {
        AuthDto.RegisterRequest regReq = new AuthDto.RegisterRequest();
        regReq.setName("User One");
        regReq.setEmail("dup@example.com");
        regReq.setPassword("secret123");
        authService.register(regReq);

        AuthDto.RegisterRequest regReq2 = new AuthDto.RegisterRequest();
        regReq2.setName("User Two");
        regReq2.setEmail("dup@example.com");
        regReq2.setPassword("secret123");

        assertThrows(IllegalArgumentException.class, () -> authService.register(regReq2));
    }

    @Test
    void testInvalidPassword() {
        AuthDto.RegisterRequest regReq = new AuthDto.RegisterRequest();
        regReq.setName("User One");
        regReq.setEmail("valid@example.com");
        regReq.setPassword("secret123");
        authService.register(regReq);

        AuthDto.LoginRequest loginReq = new AuthDto.LoginRequest();
        loginReq.setEmail("valid@example.com");
        loginReq.setPassword("wrongpassword");

        assertThrows(IllegalArgumentException.class, () -> authService.login(loginReq));
    }

    @Test
    void testCreateAndJoinRoom() {
        AuthDto.RegisterRequest regReq = new AuthDto.RegisterRequest();
        regReq.setName("Room Owner");
        regReq.setEmail("owner@example.com");
        regReq.setPassword("secret123");
        AuthDto.AuthResponse user = authService.register(regReq);

        RoomDto.RoomResponse createdRoom = roomService.createRoom(user.getUser().getId());
        assertNotNull(createdRoom.getRoomCode());
        assertEquals(6, createdRoom.getRoomCode().length());

        RoomDto.RoomResponse joinedRoom = roomService.joinRoomByCode(createdRoom.getRoomCode());
        assertEquals(createdRoom.getId(), joinedRoom.getId());
    }

    @Test
    void testInvalidRoomCode() {
        assertThrows(IllegalArgumentException.class, () -> roomService.joinRoomByCode("INVALID"));
    }
}
