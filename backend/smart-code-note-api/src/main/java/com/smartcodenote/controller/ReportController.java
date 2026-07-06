package com.smartcodenote.controller;

import com.smartcodenote.common.Result;
import com.smartcodenote.dto.LearningOverviewResponse;
import com.smartcodenote.dto.LearningSuggestionResponse;
import com.smartcodenote.dto.WeakKnowledgeResponse;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    public Result<LearningOverviewResponse> overview() {
        return Result.success(reportService.overview(CurrentUser.getUserId()));
    }

    @GetMapping("/weak-knowledge")
    public Result<List<WeakKnowledgeResponse>> weakKnowledge(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return Result.success(reportService.weakKnowledge(CurrentUser.getUserId(), limit));
    }

    @GetMapping("/suggestions")
    public Result<LearningSuggestionResponse> suggestions() {
        return Result.success(reportService.suggestions(CurrentUser.getUserId()));
    }
}
