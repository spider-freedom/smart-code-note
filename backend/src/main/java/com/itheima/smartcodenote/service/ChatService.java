package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.dto.ChatLearningContext;
import com.itheima.smartcodenote.dto.ChatMessageResponse;
import com.itheima.smartcodenote.dto.ChatSendRequest;
import com.itheima.smartcodenote.dto.ChatSessionListItem;
import com.itheima.smartcodenote.dto.ChatSessionResponse;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    List<ChatSessionListItem> listSessions(Long userId);

    ChatSessionResponse getSession(Long userId, Long sessionId);

    void deleteSession(Long userId, Long sessionId);

    void sendMessageStream(Long userId, ChatSendRequest request, SseEmitter emitter);

    void sendMessageStream(Long userId, ChatSendRequest request,
                           Consumer<String> onChunk,
                           Consumer<ChatMessageResponse> onComplete,
                           Consumer<Throwable> onError);

    ChatLearningContext getLearningContext(Long userId);
}
