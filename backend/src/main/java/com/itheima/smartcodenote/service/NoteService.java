package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.dto.NoteDetailResponse;
import com.itheima.smartcodenote.dto.NoteListItemResponse;
import com.itheima.smartcodenote.dto.NoteQueryRequest;
import com.itheima.smartcodenote.dto.NoteUploadResponse;
import com.itheima.smartcodenote.dto.NoteUploadTextRequest;
import org.springframework.web.multipart.MultipartFile;

public interface NoteService {

    NoteUploadResponse upload(Long userId, MultipartFile file, String title, String category, String tags);

    NoteUploadResponse uploadText(Long userId, NoteUploadTextRequest request);

    PageResponse<NoteListItemResponse> list(Long userId, NoteQueryRequest request);

    NoteDetailResponse detail(Long userId, Long noteId);

    void delete(Long userId, Long noteId);

    NoteDetailResponse reparse(Long userId, Long noteId);
}
