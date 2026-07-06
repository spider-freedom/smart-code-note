package com.smartcodenote.controller;

import com.smartcodenote.common.Result;
import com.smartcodenote.dto.AnswerResultResponse;
import com.smartcodenote.dto.PracticeQuestionResponse;
import com.smartcodenote.dto.StartPracticeRequest;
import com.smartcodenote.dto.SubmitAnswerRequest;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.PracticeService;
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
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/start")
    public Result<List<PracticeQuestionResponse>> start(@Valid StartPracticeRequest request) {
        return Result.success(practiceService.start(CurrentUser.getUserId(), request));
    }

    @PostMapping("/submit")
    public Result<AnswerResultResponse> submit(@Valid @RequestBody SubmitAnswerRequest request) {
        return Result.success(practiceService.submit(CurrentUser.getUserId(), request));
    }
}
