package com.smartcodenote.controller;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.common.Result;
import com.smartcodenote.dto.PracticeQuestionResponse;
import com.smartcodenote.dto.WrongQuestionDetailResponse;
import com.smartcodenote.dto.WrongQuestionListItemResponse;
import com.smartcodenote.dto.WrongQuestionQueryRequest;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.WrongQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    @GetMapping("/list")
    public Result<PageResponse<WrongQuestionListItemResponse>> list(@Valid WrongQuestionQueryRequest request) {
        return Result.success(wrongQuestionService.list(CurrentUser.getUserId(), request));
    }

    @PostMapping("/{id}/retry")
    public Result<PracticeQuestionResponse> retry(@PathVariable Long id) {
        return Result.success(wrongQuestionService.retry(CurrentUser.getUserId(), id));
    }

    @PutMapping("/{id}/mastered")
    public Result<WrongQuestionDetailResponse> markMastered(@PathVariable Long id) {
        return Result.success(wrongQuestionService.markMastered(CurrentUser.getUserId(), id));
    }
}
