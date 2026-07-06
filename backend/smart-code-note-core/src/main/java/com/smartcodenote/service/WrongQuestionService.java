package com.smartcodenote.service;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.dto.PracticeQuestionResponse;
import com.smartcodenote.dto.WrongQuestionDetailResponse;
import com.smartcodenote.dto.WrongQuestionListItemResponse;
import com.smartcodenote.dto.WrongQuestionQueryRequest;

public interface WrongQuestionService {

    PageResponse<WrongQuestionListItemResponse> list(Long userId, WrongQuestionQueryRequest request);

    PracticeQuestionResponse retry(Long userId, Long wrongQuestionId);

    WrongQuestionDetailResponse markMastered(Long userId, Long wrongQuestionId);
}
