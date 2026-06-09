package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.dto.ReviewResultResponse;
import com.itheima.smartcodenote.dto.ReviewTaskResponse;
import com.itheima.smartcodenote.dto.SubmitReviewResultRequest;
import java.util.List;

public interface ReviewPlanService {

    void recordAnswerResult(Long userId, Long knowledgeId, Long questionId, int score, boolean correct);

    List<ReviewTaskResponse> today(Long userId);

    ReviewResultResponse submit(Long userId, SubmitReviewResultRequest request);
}
