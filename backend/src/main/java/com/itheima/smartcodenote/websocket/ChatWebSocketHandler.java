package com.itheima.smartcodenote.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.smartcodenote.dto.ChatSendRequest;
import com.itheima.smartcodenote.service.ChatService;
import com.itheima.smartcodenote.util.JwtUtil;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatService chatService;
    private final JwtUtil jwtUtil;
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, JwtUtil jwtUtil) {
        this.chatService = chatService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            closeSession(session, CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "token".equals(pair[0])) {
                    token = pair[1];
                    break;
                }
            }
        }

        if (token == null || token.isBlank()) {
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        try {
            Long userId = jwtUtil.verifyAndGetUserId(token);
            sessionUserMap.put(session.getId(), userId);
            log.info("WebSocket connected: session={}, userId={}", session.getId(), userId);
        } catch (Exception e) {
            log.warn("WebSocket auth failed: {}", e.getMessage());
            closeSession(session, CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = sessionUserMap.get(session.getId());
        if (userId == null) {
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        ChatSendRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), ChatSendRequest.class);
        } catch (Exception e) {
            sendMessage(session, "error", "消息格式错误");
            return;
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            sendMessage(session, "error", "消息内容不能为空");
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                chatService.sendMessageStream(userId, request,
                        chunk -> sendMessage(session, "chunk", chunk),
                        resp -> sendMessage(session, "result", resp),
                        error -> {
                            sendMessage(session, "error", error.getMessage());
                            closeSession(session, CloseStatus.SERVER_ERROR);
                        });
            } catch (Exception e) {
                log.error("WebSocket chat error", e);
                sendMessage(session, "error", e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionUserMap.remove(session.getId());
        log.info("WebSocket disconnected: session={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionUserMap.remove(session.getId());
        log.error("WebSocket transport error: session={}", session.getId(), exception);
    }

    private void sendMessage(WebSocketSession session, String type, Object data) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", type, "content", data));
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
        }
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        if (session.isOpen()) {
            try {
                session.close(status);
            } catch (IOException e) {
                log.error("Failed to close WebSocket session", e);
            }
        }
    }
}
