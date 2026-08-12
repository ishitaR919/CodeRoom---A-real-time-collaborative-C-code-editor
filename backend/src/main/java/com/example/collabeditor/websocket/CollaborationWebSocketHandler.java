package com.example.collabeditor.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CollaborationWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoomSessionManager sessionManager;

    public CollaborationWebSocketHandler(RoomSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.has("type") ? root.get("type").asText() : "";

            if ("JOIN".equalsIgnoreCase(type)) {
                String roomId = root.get("roomId").asText();
                String userId = root.get("userId").asText();
                String userName = root.get("userName").asText();

                sessionManager.addSession(roomId, session, userId, userName);
            } else if ("YJS_UPDATE".equalsIgnoreCase(type)) {
                String roomId = root.get("roomId").asText();
                // Broadcast YJS edit vector to all other connected clients in room
                sessionManager.broadcastToRoom(roomId, message, session);
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message from session {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }
}
