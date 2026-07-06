package com.smartcodenote.controller;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.common.RateLimit;
import com.smartcodenote.common.Result;
import com.smartcodenote.dto.GenerateKnowledgeRequest;
import com.smartcodenote.dto.KnowledgeDetailResponse;
import com.smartcodenote.dto.KnowledgeListItemResponse;
import com.smartcodenote.dto.KnowledgeQueryRequest;
import com.smartcodenote.dto.UpdateKnowledgeRequest;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.KnowledgeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/list")
    public Result<PageResponse<KnowledgeListItemResponse>> list(@Valid KnowledgeQueryRequest request) {
        return Result.success(knowledgeService.list(CurrentUser.getUserId(), request));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDetailResponse> detail(@PathVariable Long id) {
        return Result.success(knowledgeService.detail(CurrentUser.getUserId(), id));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeRequest request) {
        return Result.success(knowledgeService.update(CurrentUser.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(CurrentUser.getUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Integer> batchDelete(@RequestBody @jakarta.validation.constraints.Size(min = 1, max = 100) List<Long> ids) {
        int count = knowledgeService.batchDelete(CurrentUser.getUserId(), ids);
        return Result.success(count);
    }

    @RateLimit(permits = 10, message = "AI 知识点生成过于频繁，请稍后重试")
    @PostMapping("/generate")
    public Result<List<KnowledgeDetailResponse>> generate(@Valid @RequestBody GenerateKnowledgeRequest request) {
        return Result.success(knowledgeService.generate(CurrentUser.getUserId(), request));
    }

    @RateLimit(permits = 10, message = "AI 知识点生成过于频繁，请稍后重试")
    @GetMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid GenerateKnowledgeRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Long userId = CurrentUser.getUserId();

        new Thread(() -> {
            try {
                knowledgeService.generateStream(userId, request, emitter);
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
