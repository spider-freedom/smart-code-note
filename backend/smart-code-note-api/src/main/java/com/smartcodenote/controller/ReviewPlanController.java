package com.smartcodenote.controller;

import com.smartcodenote.common.Result;
import com.smartcodenote.dto.ReviewResultResponse;
import com.smartcodenote.dto.ReviewTaskResponse;
import com.smartcodenote.dto.SubmitReviewResultRequest;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.ReviewPlanService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewPlanController {

    private final ReviewPlanService reviewPlanService;

    @GetMapping("/today")
    public Result<List<ReviewTaskResponse>> today() {
        return Result.success(reviewPlanService.today(CurrentUser.getUserId()));
    }

    @PostMapping("/submit")
    public Result<ReviewResultResponse> submit(@Valid @RequestBody SubmitReviewResultRequest request) {
        return Result.success(reviewPlanService.submit(CurrentUser.getUserId(), request));
    }
}
