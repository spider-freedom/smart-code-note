package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.dto.PracticeQuestionResponse;
import com.itheima.smartcodenote.dto.WrongQuestionDetailResponse;
import com.itheima.smartcodenote.dto.WrongQuestionListItemResponse;
import com.itheima.smartcodenote.dto.WrongQuestionQueryRequest;

public interface WrongQuestionService {

    PageResponse<WrongQuestionListItemResponse> list(Long userId, WrongQuestionQueryRequest request);

    PracticeQuestionResponse retry(Long userId, Long wrongQuestionId);

    WrongQuestionDetailResponse markMastered(Long userId, Long wrongQuestionId);
}
