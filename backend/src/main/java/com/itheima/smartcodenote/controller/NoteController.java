package com.itheima.smartcodenote.controller;

import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.common.Result;
import com.itheima.smartcodenote.dto.NoteDetailResponse;
import com.itheima.smartcodenote.dto.NoteListItemResponse;
import com.itheima.smartcodenote.dto.NoteQueryRequest;
import com.itheima.smartcodenote.dto.NoteUploadResponse;
import com.itheima.smartcodenote.dto.NoteUploadTextRequest;
import com.itheima.smartcodenote.security.CurrentUser;
import com.itheima.smartcodenote.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping("/upload")
    public Result<NoteUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags) {
        return Result.success(noteService.upload(CurrentUser.getUserId(), file, title, category, tags));
    }

    @PostMapping("/upload-text")
    public Result<NoteUploadResponse> uploadText(@Valid @RequestBody NoteUploadTextRequest request) {
        return Result.success(noteService.uploadText(CurrentUser.getUserId(), request));
    }

    @GetMapping("/list")
    public Result<PageResponse<NoteListItemResponse>> list(@Valid NoteQueryRequest request) {
        return Result.success(noteService.list(CurrentUser.getUserId(), request));
    }

    @GetMapping("/{id}")
    public Result<NoteDetailResponse> detail(@PathVariable Long id) {
        return Result.success(noteService.detail(CurrentUser.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.delete(CurrentUser.getUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/parse")
    public Result<NoteDetailResponse> parse(@PathVariable Long id) {
        return Result.success(noteService.reparse(CurrentUser.getUserId(), id));
    }
}
