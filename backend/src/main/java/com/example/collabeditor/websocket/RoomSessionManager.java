package com.example.collabeditor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomSessionManager {

    private static final Logger log = LoggerFactory.getLogger(RoomSessionManager.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map roomId -> Set of active WebSocketSessions
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // Map sessionId -> SessionInfo (roomId, userId, userName)
    private final Map<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    // Map roomId -> Map<userId, UserPresence>
    private final Map<String, Map<String, UserPresence>> roomPresenceMap = new ConcurrentHashMap<>();

    public static class SessionInfo {
        private final String roomId;
        private final String userId;
        private final String userName;

        public SessionInfo(String roomId, String userId, String userName) {
            this.roomId = roomId;
            this.userId = userId;
            this.userName = userName;
        }

        public String getRoomId() { return roomId; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
    }

    public static class UserPresence {
        private final String userId;
        private final String name;

        public UserPresence(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public String getUserId() { return userId; }
        public String getName() { return name; }
    }

    public void addSession(String roomId, WebSocketSession session, String userId, String userName) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionInfoMap.put(session.getId(), new SessionInfo(roomId, userId, userName));

        roomPresenceMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, new UserPresence(userId, userName));

        log.info("User {} ({}) joined room {}. Total sessions in room: {}", userName, userId, roomId, roomSessions.get(roomId).size());

        broadcastPresence(roomId);
    }

    public void removeSession(WebSocketSession session) {
        SessionInfo info = sessionInfoMap.remove(session.getId());
        if (info != null) {
            String roomId = info.getRoomId();
            String userId = info.getUserId();

            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomId);
                    roomPresenceMap.remove(roomId);
                } else {
                    // Check if user has other active sessions in the room
                    boolean userStillPresent = sessionInfoMap.values().stream()
                            .anyMatch(s -> s.getRoomId().equals(roomId) && s.getUserId().equals(userId));
                    if (!userStillPresent) {
                        Map<String, UserPresence> presenceMap = roomPresenceMap.get(roomId);
                        if (presenceMap != null) {
                            presenceMap.remove(userId);
                        }
                    }
                    broadcastPresence(roomId);
                }
            }
            log.info("Session {} removed from room {}", session.getId(), roomId);
        }
    }

    public void broadcastToRoom(String roomId, TextMessage message, WebSocketSession senderSession) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen() && (senderSession == null || !s.getId().equals(senderSession.getId()))) {
                    try {
                        s.sendMessage(message);
                    } catch (IOException e) {
                        log.error("Failed to send message to session {}", s.getId(), e);
                    }
                }
            }
        }
    }

    public void broadcastPresence(String roomId) {
        Map<String, UserPresence> presenceMap = roomPresenceMap.get(roomId);
        List<UserPresence> onlineUsers = presenceMap != null ? new ArrayList<>(presenceMap.values()) : Collections.emptyList();

        try {
            Map<String, Object> presenceMsg = Map.of(
                    "type", "PRESENCE",
                    "roomId", roomId,
                    "users", onlineUsers
            );
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(presenceMsg));
            broadcastToRoom(roomId, message, null);
        } catch (Exception e) {
            log.error("Failed to broadcast presence for room {}", roomId, e);
        }
    }

    public SessionInfo getSessionInfo(String sessionId) {
        return sessionInfoMap.get(sessionId);
    }
}
