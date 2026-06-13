package com.itheima.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.smartcodenote.ai.AiService;
import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.dto.GenerateKnowledgeRequest;
import com.itheima.smartcodenote.dto.KnowledgeDetailResponse;
import com.itheima.smartcodenote.dto.KnowledgeListItemResponse;
import com.itheima.smartcodenote.dto.KnowledgeQueryRequest;
import com.itheima.smartcodenote.dto.UpdateKnowledgeRequest;
import com.itheima.smartcodenote.entity.KnowledgePoint;
import com.itheima.smartcodenote.entity.Note;
import com.itheima.smartcodenote.entity.NoteChunk;
import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.mapper.KnowledgePointMapper;
import com.itheima.smartcodenote.mapper.NoteChunkMapper;
import com.itheima.smartcodenote.mapper.NoteMapper;
import com.itheima.smartcodenote.rag.EmbeddingClient;
import com.itheima.smartcodenote.rag.RagProperties;
import com.itheima.smartcodenote.service.KnowledgeService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);
    private static final int DEFAULT_MASTERY_LEVEL = 0;

    private final KnowledgePointMapper knowledgePointMapper;
    private final NoteMapper noteMapper;
    private final NoteChunkMapper noteChunkMapper;
    private final AiService aiService;
    private final RagProperties ragProperties;
    private final EmbeddingClient embeddingClient;

    @Override
    @Cacheable(value = "knowledge-list", key = "#userId + ':' + #request.noteId + ':' + #request.pageNum")
    public PageResponse<KnowledgeListItemResponse> list(Long userId, KnowledgeQueryRequest request) {
        LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getUserId, userId)
                .eq(request.getNoteId() != null, KnowledgePoint::getNoteId, request.getNoteId())
                .eq(StringUtils.hasText(request.getType()), KnowledgePoint::getType, request.getType())
                .eq(StringUtils.hasText(request.getDifficulty()), KnowledgePoint::getDifficulty, request.getDifficulty())
                .eq(request.getMasteryLevel() != null, KnowledgePoint::getMasteryLevel, request.getMasteryLevel())
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(KnowledgePoint::getTitle, request.getKeyword())
                        .or()
                        .like(KnowledgePoint::getSummary, request.getKeyword()))
                .orderByDesc(KnowledgePoint::getCreateTime);
        Page<KnowledgePoint> page = knowledgePointMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                wrapper);
        return PageResponse.of(
                page.getRecords().stream().map(this::toListItem).toList(),
                page.getTotal(),
                request.getPageNum(),
                request.getPageSize());
    }

    @Override
    public KnowledgeDetailResponse detail(Long userId, Long knowledgeId) {
        return toDetail(requireOwnedKnowledge(userId, knowledgeId));
    }

    @Override
    @CacheEvict(value = "knowledge-list", allEntries = true)
    public KnowledgeDetailResponse update(Long userId, Long knowledgeId, UpdateKnowledgeRequest request) {
        KnowledgePoint knowledgePoint = requireOwnedKnowledge(userId, knowledgeId);
        knowledgePoint.setTitle(request.getTitle().trim());
        knowledgePoint.setType(trimToNull(request.getType()));
        knowledgePoint.setSummary(trimToNull(request.getSummary()));
        knowledgePoint.setDifficulty(trimToNull(request.getDifficulty()));
        knowledgePoint.setMasteryLevel(request.getMasteryLevel() == null
                ? knowledgePoint.getMasteryLevel()
                : request.getMasteryLevel());
        knowledgePoint.setNextReviewTime(request.getNextReviewTime());
        knowledgePoint.setUpdateTime(LocalDateTime.now());
        knowledgePointMapper.updateById(knowledgePoint);
        return toDetail(knowledgePoint);
    }

    @Override
    public void delete(Long userId, Long knowledgeId) {
        requireOwnedKnowledge(userId, knowledgeId);
        knowledgePointMapper.deleteById(knowledgeId);
    }

    @Override
    public List<KnowledgeDetailResponse> generate(Long userId, GenerateKnowledgeRequest request) {
        Note note = requireOwnedNote(userId, request.getNoteId());
        List<AiService.GeneratedKnowledgePoint> generatedPoints =
                aiService.extractKnowledgePoints(note.getTitle(), note.getCleanContent());
        if (generatedPoints.isEmpty()) {
            throw new BusinessException("no knowledge points generated");
        }

        knowledgePointMapper.delete(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getUserId, userId)
                .eq(KnowledgePoint::getNoteId, note.getId()));

        LocalDateTime now = LocalDateTime.now();
        return generatedPoints.stream()
                .map(point -> createKnowledgePoint(userId, note.getId(), point, now))
                .map(this::toDetail)
                .toList();
    }

    @Override
    public void generateStream(Long userId, GenerateKnowledgeRequest request, SseEmitter emitter) {
        Note note = requireOwnedNote(userId, request.getNoteId());

        aiService.extractKnowledgePointsStream(
                note.getTitle(),
                note.getCleanContent(),
                chunk -> sendSse(emitter, "chunk", chunk),
                generatedPoints -> {
                    if (generatedPoints.isEmpty()) {
                        sendSse(emitter, "error", "AI未提取到知识点");
                        emitter.complete();
                        return;
                    }

                    knowledgePointMapper.delete(new LambdaQueryWrapper<KnowledgePoint>()
                            .eq(KnowledgePoint::getUserId, userId)
                            .eq(KnowledgePoint::getNoteId, note.getId()));

                    LocalDateTime now = LocalDateTime.now();
                    List<KnowledgeDetailResponse> result = generatedPoints.stream()
                            .map(point -> createKnowledgePoint(userId, note.getId(), point, now))
                            .map(this::toDetail)
                            .toList();

                    sendSse(emitter, "result", result);
                    emitter.complete();
                },
                error -> {
                    sendSse(emitter, "error", error.getMessage());
                    emitter.completeWithError(error);
                });
    }

    private KnowledgePoint createKnowledgePoint(
            Long userId,
            Long noteId,
            AiService.GeneratedKnowledgePoint generatedPoint,
            LocalDateTime now) {
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setUserId(userId);
        knowledgePoint.setNoteId(noteId);
        knowledgePoint.setTitle(trimToDefault(generatedPoint.title(), "Untitled knowledge point"));
        knowledgePoint.setType(trimToDefault(generatedPoint.type(), "concept"));
        knowledgePoint.setSummary(trimToNull(generatedPoint.summary()));
        knowledgePoint.setDifficulty(trimToDefault(generatedPoint.difficulty(), "medium"));
        knowledgePoint.setMasteryLevel(DEFAULT_MASTERY_LEVEL);
        knowledgePoint.setCreateTime(now);
        knowledgePoint.setUpdateTime(now);
        knowledgePoint.setDeleted(0);
        knowledgePointMapper.insert(knowledgePoint);

        // Async RAG indexing
        indexKnowledgeAsync(knowledgePoint);

        return knowledgePoint;
    }

    private Note requireOwnedNote(Long userId, Long noteId) {
        Note note = noteMapper.selectOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .last("LIMIT 1"));
        if (note == null) {
            throw new BusinessException(404, "note not found");
        }
        if (!StringUtils.hasText(note.getCleanContent())) {
            throw new BusinessException("note has no parsed content");
        }
        return note;
    }

    private KnowledgePoint requireOwnedKnowledge(Long userId, Long knowledgeId) {
        KnowledgePoint knowledgePoint = knowledgePointMapper.selectOne(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getId, knowledgeId)
                .eq(KnowledgePoint::getUserId, userId)
                .last("LIMIT 1"));
        if (knowledgePoint == null) {
            throw new BusinessException(404, "knowledge point not found");
        }
        return knowledgePoint;
    }

    private KnowledgeListItemResponse toListItem(KnowledgePoint knowledgePoint) {
        return KnowledgeListItemResponse.builder()
                .id(knowledgePoint.getId())
                .noteId(knowledgePoint.getNoteId())
                .title(knowledgePoint.getTitle())
                .type(knowledgePoint.getType())
                .difficulty(knowledgePoint.getDifficulty())
                .masteryLevel(knowledgePoint.getMasteryLevel())
                .nextReviewTime(knowledgePoint.getNextReviewTime())
                .createTime(knowledgePoint.getCreateTime())
                .build();
    }

    private KnowledgeDetailResponse toDetail(KnowledgePoint knowledgePoint) {
        return KnowledgeDetailResponse.builder()
                .id(knowledgePoint.getId())
                .noteId(knowledgePoint.getNoteId())
                .title(knowledgePoint.getTitle())
                .type(knowledgePoint.getType())
                .summary(knowledgePoint.getSummary())
                .difficulty(knowledgePoint.getDifficulty())
                .masteryLevel(knowledgePoint.getMasteryLevel())
                .nextReviewTime(knowledgePoint.getNextReviewTime())
                .createTime(knowledgePoint.getCreateTime())
                .updateTime(knowledgePoint.getUpdateTime())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * Asynchronously embed and store knowledge point for RAG retrieval.
     */
    private void indexKnowledgeAsync(KnowledgePoint knowledgePoint) {
        if (!ragProperties.isEnabled()) return;
        if (!StringUtils.hasText(knowledgePoint.getSummary())) return;

        CompletableFuture.runAsync(() -> {
            try {
                String text = knowledgePoint.getTitle() + "\n" + knowledgePoint.getSummary();
                float[] embedding = embeddingClient.embed(text);

                NoteChunk chunk = new NoteChunk();
                chunk.setUserId(knowledgePoint.getUserId());
                chunk.setNoteId(knowledgePoint.getNoteId());
                chunk.setKnowledgeId(knowledgePoint.getId());
                chunk.setChunkIndex(0);
                chunk.setContent(text);
                chunk.setEmbedding(EmbeddingClient.serializeVector(embedding));
                chunk.setTokenCount(text.length());
                chunk.setCreateTime(LocalDateTime.now());
                noteChunkMapper.insert(chunk);

                log.debug("RAG indexed knowledge point {} for user {}", knowledgePoint.getId(), knowledgePoint.getUserId());
            } catch (Exception e) {
                log.error("RAG indexing failed for knowledge point {}", knowledgePoint.getId(), e);
            }
        });
    }

    private void sendSse(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
