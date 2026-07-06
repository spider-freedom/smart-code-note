package com.smartcodenote.service;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.dto.NoteDetailResponse;
import com.smartcodenote.dto.NoteListItemResponse;
import com.smartcodenote.dto.NoteQueryRequest;
import com.smartcodenote.dto.NoteUploadResponse;
import com.smartcodenote.dto.NoteUploadTextRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface NoteService {

    NoteUploadResponse upload(Long userId, MultipartFile file, String title, String category, String tags);

    NoteUploadResponse uploadText(Long userId, NoteUploadTextRequest request);

    PageResponse<NoteListItemResponse> list(Long userId, NoteQueryRequest request);

    NoteDetailResponse detail(Long userId, Long noteId);

    void delete(Long userId, Long noteId);

    /**
     * Batch delete notes owned by user. Returns count of actually deleted rows.
     */
    int batchDelete(Long userId, List<Long> noteIds);

    NoteDetailResponse reparse(Long userId, Long noteId);
}
