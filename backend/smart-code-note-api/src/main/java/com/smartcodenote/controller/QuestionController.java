package com.smartcodenote.controller;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.common.RateLimit;
import com.smartcodenote.common.Result;
import com.smartcodenote.dto.GenerateQuestionRequest;
import com.smartcodenote.dto.QuestionDetailResponse;
import com.smartcodenote.dto.QuestionListItemResponse;
import com.smartcodenote.dto.QuestionQueryRequest;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.QuestionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @RateLimit(permits = 10, message = "AI 题目生成过于频繁，请稍后重试")
    @PostMapping("/generate")
    public Result<List<QuestionDetailResponse>> generate(@Valid @RequestBody GenerateQuestionRequest request) {
        return Result.success(questionService.generate(CurrentUser.getUserId(), request));
    }

    @GetMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid GenerateQuestionRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Long userId = CurrentUser.getUserId();

        new Thread(() -> {
            try {
                questionService.generateStream(userId, request, emitter);
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

    @GetMapping("/list")
    public Result<PageResponse<QuestionListItemResponse>> list(@Valid QuestionQueryRequest request) {
        return Result.success(questionService.list(CurrentUser.getUserId(), request));
    }

    @GetMapping("/{id}")
    public Result<QuestionDetailResponse> detail(@PathVariable Long id) {
        return Result.success(questionService.detail(CurrentUser.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(CurrentUser.getUserId(), id);
        return Result.success();
    }
}
