package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.ai.AiService;
import com.itheima.smartcodenote.entity.KnowledgePoint;
import com.itheima.smartcodenote.entity.Note;
import com.itheima.smartcodenote.entity.NoteChunk;
import com.itheima.smartcodenote.entity.NoteParseTask;
import com.itheima.smartcodenote.entity.Question;
import com.itheima.smartcodenote.entity.QuestionOption;
import com.itheima.smartcodenote.mapper.KnowledgePointMapper;
import com.itheima.smartcodenote.mapper.NoteChunkMapper;
import com.itheima.smartcodenote.mapper.NoteMapper;
import com.itheima.smartcodenote.mapper.NoteParseTaskMapper;
import com.itheima.smartcodenote.mapper.QuestionMapper;
import com.itheima.smartcodenote.mapper.QuestionOptionMapper;
import com.itheima.smartcodenote.rag.EmbeddingClient;
import com.itheima.smartcodenote.rag.RagProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Handles async AI generation tasks (knowledge extraction + question generation).
 *
 * Design notes:
 * - Uses a bounded thread pool (3 workers) to avoid overwhelming the DeepSeek API.
 * - Task state is persisted to note_parse_task table so it survives restarts.
 * - Production would replace the thread pool with a message queue (RocketMQ / Kafka)
 *   for durability, retries, and horizontal scaling.
 */
@Service
@RequiredArgsConstructor
public class NoteAsyncService {

    private static final Logger log = LoggerFactory.getLogger(NoteAsyncService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int DEFAULT_MASTERY_LEVEL = 0;
    private static final int DEFAULT_QUESTION_COUNT = 5;

    /**
     * Bounded thread pool — limits concurrent AI API calls.
     * Production: replace with message queue for durability and retry.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "note-parse-worker");
        t.setDaemon(true);
        return t;
    });

    private final NoteParseTaskMapper taskMapper;
    private final NoteMapper noteMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final NoteChunkMapper noteChunkMapper;
    private final AiService aiService;
    private final RagProperties ragProperties;
    private final EmbeddingClient embeddingClient;

    /**
     * Submit an async AI generation task for a note.
     * Returns immediately with the taskId; AI processing happens in background.
     */
    public NoteParseTask submitTask(Long userId, Long noteId) {
        LocalDateTime now = LocalDateTime.now();
        NoteParseTask task = new NoteParseTask();
        task.setUserId(userId);
        task.setNoteId(noteId);
        task.setStatus(STATUS_PENDING);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        taskMapper.insert(task);

        try {
            executor.submit(() -> processTask(task));
        } catch (RejectedExecutionException e) {
            task.setStatus(STATUS_FAILED);
            task.setErrorMessage("System busy, please retry later");
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
            log.warn("Async task rejected for note {} (user {})", noteId, userId);
        }

        return task;
    }

    /**
     * Query the current status of a parse task.
     */
    public NoteParseTask getTaskStatus(Long noteId) {
        return taskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteParseTask>()
                        .eq(NoteParseTask::getNoteId, noteId)
                        .orderByDesc(NoteParseTask::getCreateTime)
                        .last("LIMIT 1"));
    }

    /**
     * Main processing logic — runs in background thread.
     */
    private void processTask(NoteParseTask task) {
        log.info("Starting async parsing for note {} (user {})", task.getNoteId(), task.getUserId());

        try {
            // 1. Mark as processing
            task.setStatus(STATUS_PROCESSING);
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);

            // 2. Load note content
            Note note = noteMapper.selectById(task.getNoteId());
            if (note == null || !StringUtils.hasText(note.getCleanContent())) {
                failTask(task, "Note content is empty");
                return;
            }

            // 3. AI knowledge extraction (the slow part — ~15-30s)
            List<AiService.GeneratedKnowledgePoint> generatedPoints =
                    aiService.extractKnowledgePoints(note.getTitle(), note.getCleanContent());

            if (generatedPoints.isEmpty()) {
                failTask(task, "AI could not extract any knowledge points");
                return;
            }

            // 4. Save knowledge points
            LocalDateTime now = LocalDateTime.now();
            List<KnowledgePoint> savedPoints = new java.util.ArrayList<>();
            for (AiService.GeneratedKnowledgePoint gp : generatedPoints) {
                KnowledgePoint kp = new KnowledgePoint();
                kp.setUserId(task.getUserId());
                kp.setNoteId(task.getNoteId());
                kp.setTitle(StringUtils.hasText(gp.title()) ? gp.title().trim() : "Untitled");
                kp.setType(StringUtils.hasText(gp.type()) ? gp.type().trim() : "concept");
                kp.setSummary(StringUtils.hasText(gp.summary()) ? gp.summary().trim() : null);
                kp.setDifficulty(StringUtils.hasText(gp.difficulty()) ? gp.difficulty().trim() : "medium");
                kp.setMasteryLevel(DEFAULT_MASTERY_LEVEL);
                kp.setCreateTime(now);
                kp.setUpdateTime(now);
                kp.setDeleted(0);
                knowledgePointMapper.insert(kp);
                savedPoints.add(kp);

                // Async RAG indexing per knowledge point
                indexKnowledgeAsync(kp);
            }

            task.setKnowledgeCount(savedPoints.size());
            log.info("Generated {} knowledge points for note {}", savedPoints.size(), task.getNoteId());

            // 5. AI question generation per knowledge point
            int totalQuestions = 0;
            for (KnowledgePoint kp : savedPoints) {
                List<AiService.GeneratedQuestion> generatedQuestions =
                        aiService.generateQuestions(kp.getTitle(), kp.getSummary(), DEFAULT_QUESTION_COUNT);

                for (AiService.GeneratedQuestion gq : generatedQuestions) {
                    Question q = new Question();
                    q.setUserId(task.getUserId());
                    q.setNoteId(task.getNoteId());
                    q.setKnowledgeId(kp.getId());
                    q.setQuestionType(gq.questionType());
                    q.setContent(gq.content());
                    q.setStandardAnswer(gq.standardAnswer());
                    q.setAnalysis(gq.analysis());
                    q.setDifficulty(gq.difficulty());
                    q.setCreateTime(LocalDateTime.now());
                    q.setUpdateTime(LocalDateTime.now());
                    q.setDeleted(0);
                    questionMapper.insert(q);

                    // Save options for choice/ judgement questions
                    if (gq.options() != null && !gq.options().isEmpty()) {
                        for (AiService.GeneratedQuestionOption opt : gq.options()) {
                            QuestionOption qo = new QuestionOption();
                            qo.setQuestionId(q.getId());
                            qo.setOptionKey(opt.optionKey());
                            qo.setOptionContent(opt.optionContent());
                            qo.setCorrect(opt.correct() ? 1 : 0);
                            questionOptionMapper.insert(qo);
                        }
                    }
                    totalQuestions++;
                }
            }

            // 6. Mark completed
            task.setStatus(STATUS_COMPLETED);
            task.setQuestionCount(totalQuestions);
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);

            log.info("Async parsing completed for note {}: {} knowledge, {} questions",
                    task.getNoteId(), savedPoints.size(), totalQuestions);

        } catch (Exception e) {
            log.error("Async parsing failed for note {}", task.getNoteId(), e);
            failTask(task, e.getMessage());
        }
    }

    private void failTask(NoteParseTask task, String errorMessage) {
        task.setStatus(STATUS_FAILED);
        task.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500) : errorMessage);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /**
     * Async RAG indexing for a knowledge point.
     */
    private void indexKnowledgeAsync(KnowledgePoint knowledgePoint) {
        if (!ragProperties.isEnabled()) return;
        if (!StringUtils.hasText(knowledgePoint.getSummary())) return;

        java.util.concurrent.CompletableFuture.runAsync(() -> {
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
            } catch (Exception e) {
                log.error("RAG indexing failed for knowledge point {}", knowledgePoint.getId(), e);
            }
        });
    }
}
