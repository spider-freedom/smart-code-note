package com.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcodenote.common.PageResponse;
import com.smartcodenote.config.properties.FileStorageProperties;
import com.smartcodenote.dto.NoteDetailResponse;
import com.smartcodenote.dto.NoteListItemResponse;
import com.smartcodenote.dto.NoteQueryRequest;
import com.smartcodenote.dto.NoteUploadResponse;
import com.smartcodenote.dto.NoteUploadTextRequest;
import com.smartcodenote.entity.Note;
import com.smartcodenote.entity.NoteChunk;
import com.smartcodenote.exception.BusinessException;
import com.smartcodenote.mapper.NoteChunkMapper;
import com.smartcodenote.mapper.NoteMapper;
import com.smartcodenote.rag.ChunkingService;
import com.smartcodenote.rag.EmbeddingClient;
import com.smartcodenote.rag.RagProperties;
import com.smartcodenote.service.NoteParserService;
import com.smartcodenote.service.NoteService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteServiceImpl.class);
    private static final int PARSE_SUCCESS = 1;

    private final NoteMapper noteMapper;
    private final NoteChunkMapper noteChunkMapper;
    private final NoteParserService noteParserService;
    private final FileStorageProperties fileStorageProperties;
    private final RagProperties ragProperties;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;

    @Override
    public NoteUploadResponse uploadText(Long userId, NoteUploadTextRequest request) {
        NoteParserService.ParsedNote parsedNote = noteParserService.parseContent(request.getContent());

        LocalDateTime now = LocalDateTime.now();
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.getTitle().trim());
        note.setCategory(trimToNull(request.getCategory()));
        note.setTags(trimToNull(request.getTags()));
        note.setFileUrl(null);
        note.setFileType(parsedNote.fileType());
        note.setOriginalContent(parsedNote.originalContent());
        note.setCleanContent(parsedNote.cleanContent());
        note.setParseStatus(PARSE_SUCCESS);
        note.setCreateTime(now);
        note.setUpdateTime(now);
        note.setDeleted(0);
        noteMapper.insert(note);

        // Async RAG indexing
        indexNoteAsync(note);

        return NoteUploadResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .fileType(note.getFileType())
                .parseStatus(note.getParseStatus())
                .originalLength(note.getOriginalContent().length())
                .cleanLength(note.getCleanContent().length())
                .build();
    }

    @Override
    public NoteUploadResponse upload(Long userId, MultipartFile file, String title, String category, String tags) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("title is required");
        }
        NoteParserService.ParsedNote parsedNote = noteParserService.parse(file);
        String fileUrl = saveFile(file, parsedNote.fileType());

        LocalDateTime now = LocalDateTime.now();
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title.trim());
        note.setCategory(trimToNull(category));
        note.setTags(trimToNull(tags));
        note.setFileUrl(fileUrl);
        note.setFileType(parsedNote.fileType());
        note.setOriginalContent(parsedNote.originalContent());
        note.setCleanContent(parsedNote.cleanContent());
        note.setParseStatus(PARSE_SUCCESS);
        note.setCreateTime(now);
        note.setUpdateTime(now);
        note.setDeleted(0);
        noteMapper.insert(note);

        // Async RAG indexing
        indexNoteAsync(note);

        return NoteUploadResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .fileType(note.getFileType())
                .parseStatus(note.getParseStatus())
                .originalLength(note.getOriginalContent().length())
                .cleanLength(note.getCleanContent().length())
                .build();
    }

    @Override
    public PageResponse<NoteListItemResponse> list(Long userId, NoteQueryRequest request) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(StringUtils.hasText(request.getCategory()), Note::getCategory, request.getCategory())
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(Note::getTitle, request.getKeyword())
                        .or()
                        .like(Note::getTags, request.getKeyword()))
                .orderByDesc(Note::getCreateTime);
        Page<Note> page = noteMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        return PageResponse.of(
                page.getRecords().stream().map(this::toListItem).toList(),
                page.getTotal(),
                request.getPageNum(),
                request.getPageSize());
    }

    @Override
    public NoteDetailResponse detail(Long userId, Long noteId) {
        return toDetail(requireOwnedNote(userId, noteId));
    }

    @Override
    public void delete(Long userId, Long noteId) {
        requireOwnedNote(userId, noteId);
        noteMapper.deleteById(noteId);
    }

    @Override
    public int batchDelete(Long userId, List<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return 0;
        }
        // Verify ownership: only delete notes owned by this user
        List<Note> notes = noteMapper.selectBatchIds(noteIds);
        List<Long> ownedIds = notes.stream()
                .filter(n -> n.getUserId().equals(userId))
                .map(Note::getId)
                .toList();
        if (ownedIds.isEmpty()) {
            return 0;
        }
        // MyBatis-Plus deleteBatchIds generates DELETE ... WHERE id IN (...)
        return noteMapper.deleteBatchIds(ownedIds);
    }

    @Override
    public NoteDetailResponse reparse(Long userId, Long noteId) {
        Note note = requireOwnedNote(userId, noteId);
        byte[] bytes = readOriginalBytes(note);
        NoteParserService.ParsedNote parsedNote = noteParserService.parse(note.getFileUrl(), bytes);
        note.setOriginalContent(parsedNote.originalContent());
        note.setCleanContent(parsedNote.cleanContent());
        note.setFileType(parsedNote.fileType());
        note.setParseStatus(PARSE_SUCCESS);
        note.setUpdateTime(LocalDateTime.now());
        noteMapper.updateById(note);

        // Async RAG re-indexing
        indexNoteAsync(note);

        return toDetail(note);
    }

    /**
     * Asynchronously chunk, embed, and store note content for RAG retrieval.
     */
    private void indexNoteAsync(Note note) {
        if (!ragProperties.isEnabled()) return;
        if (!StringUtils.hasText(note.getCleanContent())) return;

        CompletableFuture.runAsync(() -> {
            try {
                // Delete old chunks for this note
                noteChunkMapper.deleteByNoteId(note.getId());

                List<String> chunks = chunkingService.chunk(note.getCleanContent());
                if (chunks.isEmpty()) return;

                List<float[]> embeddings = embeddingClient.embedBatch(chunks);
                if (embeddings.size() != chunks.size()) {
                    log.warn("Embedding count mismatch: chunks={}, embeddings={}", chunks.size(), embeddings.size());
                    return;
                }

                LocalDateTime now = LocalDateTime.now();
                for (int i = 0; i < chunks.size(); i++) {
                    NoteChunk noteChunk = new NoteChunk();
                    noteChunk.setUserId(note.getUserId());
                    noteChunk.setNoteId(note.getId());
                    noteChunk.setChunkIndex(i);
                    noteChunk.setContent(chunks.get(i));
                    noteChunk.setEmbedding(EmbeddingClient.serializeVector(embeddings.get(i)));
                    noteChunk.setTokenCount(chunks.get(i).length());
                    noteChunk.setCreateTime(now);
                    noteChunkMapper.insert(noteChunk);
                }

                log.info("RAG indexed {} chunks for note {} (user={})", chunks.size(), note.getId(), note.getUserId());
            } catch (Exception e) {
                log.error("RAG indexing failed for note {}", note.getId(), e);
            }
        });
    }

    private String saveFile(MultipartFile file, String fileType) {
        try {
            Path uploadRoot = Path.of(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);
            String storedName = UUID.randomUUID() + "." + fileType;
            Path target = uploadRoot.resolve(storedName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new BusinessException("invalid file path");
            }
            file.transferTo(target);
            return uploadRoot.relativize(target).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new BusinessException("failed to save uploaded file");
        }
    }

    private byte[] readOriginalBytes(Note note) {
        Path uploadRoot = Path.of(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path target = uploadRoot.resolve(note.getFileUrl()).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException("invalid file path");
        }
        if (Files.exists(target)) {
            try {
                return Files.readAllBytes(target);
            } catch (IOException e) {
                throw new BusinessException("failed to read note file");
            }
        }
        return note.getOriginalContent().getBytes(StandardCharsets.UTF_8);
    }

    private Note requireOwnedNote(Long userId, Long noteId) {
        Note note = noteMapper.selectOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .last("LIMIT 1"));
        if (note == null) {
            throw new BusinessException(404, "note not found");
        }
        return note;
    }

    private NoteListItemResponse toListItem(Note note) {
        return NoteListItemResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .category(note.getCategory())
                .tags(note.getTags())
                .fileType(note.getFileType())
                .parseStatus(note.getParseStatus())
                .createTime(note.getCreateTime())
                .updateTime(note.getUpdateTime())
                .build();
    }

    private NoteDetailResponse toDetail(Note note) {
        return NoteDetailResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .category(note.getCategory())
                .tags(note.getTags())
                .fileUrl(note.getFileUrl())
                .fileType(note.getFileType())
                .originalContent(note.getOriginalContent())
                .cleanContent(note.getCleanContent())
                .parseStatus(note.getParseStatus())
                .createTime(note.getCreateTime())
                .updateTime(note.getUpdateTime())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
