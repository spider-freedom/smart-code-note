package com.itheima.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.smartcodenote.ai.AiService;
import com.itheima.smartcodenote.dto.ChatLearningContext;
import com.itheima.smartcodenote.dto.ChatMessageResponse;
import com.itheima.smartcodenote.dto.ChatSendRequest;
import com.itheima.smartcodenote.dto.ChatSessionListItem;
import com.itheima.smartcodenote.dto.ChatSessionResponse;
import com.itheima.smartcodenote.entity.AnswerRecord;
import com.itheima.smartcodenote.entity.ChatMessage;
import com.itheima.smartcodenote.entity.ChatSession;
import com.itheima.smartcodenote.entity.KnowledgePoint;
import com.itheima.smartcodenote.entity.Note;
import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.mapper.AnswerRecordMapper;
import com.itheima.smartcodenote.mapper.ChatMessageMapper;
import com.itheima.smartcodenote.mapper.ChatSessionMapper;
import com.itheima.smartcodenote.mapper.KnowledgePointMapper;
import com.itheima.smartcodenote.mapper.NoteMapper;
import com.itheima.smartcodenote.service.ChatService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String STUDY_BUDDY_PROMPT = """
            你叫「小码」，是一位编程学习伙伴。你性格开朗、有耐心，擅长用简单易懂的方式讲解技术概念。
            你会主动关心用户的学习进度，在他遇到困难时给予鼓励，在他取得进步时真心为他高兴。
            你可以用技术知识回答问题，也可以用朋友的口吻聊聊天、给建议。
            你的回复简洁、温暖，像好朋友之间的对话。回复控制在150字以内。""";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final NoteMapper noteMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final AiService aiService;

    @Override
    public List<ChatSessionListItem> listSessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime))
                .stream()
                .map(s -> ChatSessionListItem.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .createTime(s.getCreateTime())
                        .updateTime(s.getUpdateTime())
                        .build())
                .toList();
    }

    @Override
    public ChatSessionResponse getSession(Long userId, Long sessionId) {
        ChatSession session = requireOwnedSession(userId, sessionId);
        List<ChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));
        return ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .messages(messages.stream().map(this::toMessageResponse).toList())
                .build();
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        sessionMapper.deleteById(sessionId);
    }

    @Override
    public void sendMessageStream(Long userId, ChatSendRequest request, SseEmitter emitter) {
        sendMessageStream(userId, request,
                chunk -> sendSse(emitter, "chunk", chunk),
                resp -> {
                    sendSse(emitter, "result", resp);
                    emitter.complete();
                },
                error -> {
                    sendSse(emitter, "error", error.getMessage());
                    emitter.completeWithError(error);
                });
    }

    @Override
    public void sendMessageStream(Long userId, ChatSendRequest request,
                                  Consumer<String> onChunk,
                                  Consumer<ChatMessageResponse> onComplete,
                                  Consumer<Throwable> onError) {
        Long sessionId = request.getSessionId();
        if (sessionId == null) {
            ChatSession session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(generateTitle(request.getMessage()));
            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            session.setDeleted(0);
            sessionMapper.insert(session);
            sessionId = session.getId();
        }

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setDeleted(0);
        messageMapper.insert(userMessage);

        List<ChatMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));
        String historyText = history.stream()
                .map(m -> (m.getRole().equals("user") ? "用户" : "小码") + "：" + m.getContent())
                .collect(Collectors.joining("\n"));

        String contextText = buildLearningContextText(userId);
        String systemPrompt = STUDY_BUDDY_PROMPT + "\n" + contextText;
        String userPrompt = historyText + "\n用户：" + request.getMessage();

        final Long finalSessionId = sessionId;

        aiService.chatStream(
                systemPrompt,
                userPrompt,
                onChunk,
                fullReply -> {
                    ChatMessage assistantMessage = new ChatMessage();
                    assistantMessage.setSessionId(finalSessionId);
                    assistantMessage.setUserId(userId);
                    assistantMessage.setRole("assistant");
                    assistantMessage.setContent(fullReply);
                    assistantMessage.setCreateTime(LocalDateTime.now());
                    assistantMessage.setDeleted(0);
                    messageMapper.insert(assistantMessage);

                    ChatSession session = sessionMapper.selectById(finalSessionId);
                    if (session != null) {
                        session.setUpdateTime(LocalDateTime.now());
                        if (session.getTitle().equals("新对话")) {
                            session.setTitle(generateTitle(fullReply));
                        }
                        sessionMapper.updateById(session);
                    }

                    ChatMessageResponse resp = ChatMessageResponse.builder()
                            .id(assistantMessage.getId())
                            .sessionId(finalSessionId)
                            .role("assistant")
                            .content(fullReply)
                            .createTime(assistantMessage.getCreateTime())
                            .build();

                    onComplete.accept(resp);
                },
                onError);
    }

    @Override
    public ChatLearningContext getLearningContext(Long userId) {
        long totalNotes = noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId));

        long totalKnowledge = knowledgePointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getUserId, userId));

        long mastered = knowledgePointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getUserId, userId)
                .ge(KnowledgePoint::getMasteryLevel, 4));

        long reviewDue = knowledgePointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getUserId, userId)
                .le(KnowledgePoint::getNextReviewTime, LocalDateTime.now()));

        LocalDate today = LocalDate.now();
        List<AnswerRecord> todayRecords = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .ge(AnswerRecord::getCreateTime, today.atStartOfDay()));

        long correctCount = todayRecords.stream()
                .filter(r -> r.getCorrect() != null && r.getCorrect() == 1)
                .count();

        double correctRate = todayRecords.isEmpty()
                ? 0
                : Math.round((double) correctCount / todayRecords.size() * 10000.0) / 100.0;

        Note recentNote = noteMapper.selectOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getUpdateTime)
                .last("LIMIT 1"));

        return ChatLearningContext.builder()
                .totalNotes((int) totalNotes)
                .totalKnowledgePoints((int) totalKnowledge)
                .masteredKnowledgePoints((int) mastered)
                .reviewDueCount((int) reviewDue)
                .todayPracticeCount(todayRecords.size())
                .todayCorrectRate(correctRate)
                .recentNoteTitle(recentNote != null ? recentNote.getTitle() : null)
                .build();
    }

    private String buildLearningContextText(Long userId) {
        ChatLearningContext ctx = getLearningContext(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("[用户学习状态]");
        if (ctx.getRecentNoteTitle() != null) {
            sb.append("\n- 最近在学: ").append(ctx.getRecentNoteTitle());
        }
        sb.append("\n- 知识掌握: 已掌握 ").append(ctx.getMasteredKnowledgePoints())
                .append(" 个知识点，共 ").append(ctx.getTotalKnowledgePoints()).append(" 个");

        if (ctx.getReviewDueCount() > 0) {
            sb.append("\n- 待复习: ").append(ctx.getReviewDueCount()).append(" 个知识点");
        }

        if (ctx.getTodayPracticeCount() > 0) {
            sb.append("\n- 今日练习: 完成 ").append(ctx.getTodayPracticeCount())
                    .append(" 题，正确率 ").append(ctx.getTodayCorrectRate()).append("%");
        } else {
            sb.append("\n- 今日尚未开始练习");
        }

        return sb.toString();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .createTime(message.getCreateTime())
                .build();
    }

    private ChatSession requireOwnedSession(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        return session;
    }

    private String generateTitle(String message) {
        String trimmed = message.trim();
        return trimmed.length() > 20 ? trimmed.substring(0, 20) + "..." : trimmed;
    }

    private void sendSse(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
