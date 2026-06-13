package com.itheima.smartcodenote.controller;

import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.common.RateLimit;
import com.itheima.smartcodenote.common.Result;
import com.itheima.smartcodenote.dto.NoteDetailResponse;
import com.itheima.smartcodenote.dto.NoteListItemResponse;
import com.itheima.smartcodenote.dto.NoteQueryRequest;
import com.itheima.smartcodenote.dto.NoteUploadResponse;
import com.itheima.smartcodenote.dto.NoteUploadTextRequest;
import com.itheima.smartcodenote.dto.ParseStatusResponse;
import com.itheima.smartcodenote.entity.NoteParseTask;
import com.itheima.smartcodenote.security.CurrentUser;
import com.itheima.smartcodenote.service.NoteAsyncService;
import com.itheima.smartcodenote.service.NoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
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
    private final NoteAsyncService noteAsyncService;

    @RateLimit(permits = 20, message = "上传过于频繁，请稍后重试")
    @PostMapping("/upload")
    public Result<NoteUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags) {
        NoteUploadResponse response = noteService.upload(CurrentUser.getUserId(), file, title, category, tags);
        // Submit async AI parsing — returns immediately
        NoteParseTask task = noteAsyncService.submitTask(CurrentUser.getUserId(), response.getId());
        response.setTaskId(task.getId());
        return Result.success(response);
    }

    @PostMapping("/upload-text")
    public Result<NoteUploadResponse> uploadText(@Valid @RequestBody NoteUploadTextRequest request) {
        NoteUploadResponse response = noteService.uploadText(CurrentUser.getUserId(), request);
        // Submit async AI parsing — returns immediately
        NoteParseTask task = noteAsyncService.submitTask(CurrentUser.getUserId(), response.getId());
        response.setTaskId(task.getId());
        return Result.success(response);
    }

    /**
     * Poll async AI parse task status.
     * Frontend calls this every 2 seconds until status is COMPLETED or FAILED.
     */
    @GetMapping("/{id}/parse-status")
    public Result<ParseStatusResponse> getParseStatus(@PathVariable Long id) {
        NoteParseTask task = noteAsyncService.getTaskStatus(id);
        if (task == null) {
            return Result.success(ParseStatusResponse.builder()
                    .status("NOT_FOUND")
                    .knowledgeCount(0)
                    .questionCount(0)
                    .build());
        }
        return Result.success(ParseStatusResponse.builder()
                .status(task.getStatus())
                .knowledgeCount(task.getKnowledgeCount() != null ? task.getKnowledgeCount() : 0)
                .questionCount(task.getQuestionCount() != null ? task.getQuestionCount() : 0)
                .errorMessage(task.getErrorMessage())
                .build());
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
        NoteDetailResponse response = noteService.reparse(CurrentUser.getUserId(), id);
        // Re-submit async AI task for re-parse
        noteAsyncService.submitTask(CurrentUser.getUserId(), id);
        return Result.success(response);
    }

    /**
     * Batch delete notes. Uses single DELETE ... WHERE id IN (...) to reduce DB round-trips.
     */
    @DeleteMapping("/batch")
    public Result<Integer> batchDelete(
            @RequestBody @Size(min = 1, max = 100, message = "单次批量删除须在 1-100 条之间")
            List<Long> ids) {
        int count = noteService.batchDelete(CurrentUser.getUserId(), ids);
        return Result.success(count);
    }
}
