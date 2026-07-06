package com.smartcodenote.service;

import com.smartcodenote.dto.ReviewResultResponse;
import com.smartcodenote.dto.ReviewTaskResponse;
import com.smartcodenote.dto.SubmitReviewResultRequest;
import java.util.List;

public interface ReviewPlanService {

    void recordAnswerResult(Long userId, Long knowledgeId, Long questionId, int score, boolean correct);

    List<ReviewTaskResponse> today(Long userId);

    ReviewResultResponse submit(Long userId, SubmitReviewResultRequest request);
}
