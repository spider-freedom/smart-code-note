package com.smartcodenote.controller;

import com.smartcodenote.common.Result;
import com.smartcodenote.dto.ChatLearningContext;
import com.smartcodenote.dto.ChatSendRequest;
import com.smartcodenote.dto.ChatSessionListItem;
import com.smartcodenote.dto.ChatSessionResponse;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/sessions")
    public Result<List<ChatSessionListItem>> listSessions() {
        return Result.success(chatService.listSessions(CurrentUser.getUserId()));
    }

    @GetMapping("/sessions/{id}")
    public Result<ChatSessionResponse> getSession(@PathVariable Long id) {
        return Result.success(chatService.getSession(CurrentUser.getUserId(), id));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(CurrentUser.getUserId(), id);
        return Result.success();
    }

    @GetMapping("/context")
    public Result<ChatLearningContext> getLearningContext() {
        return Result.success(chatService.getLearningContext(CurrentUser.getUserId()));
    }

    @GetMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@Valid ChatSendRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Long userId = CurrentUser.getUserId();

        new Thread(() -> {
            try {
                chatService.sendMessageStream(userId, request, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
