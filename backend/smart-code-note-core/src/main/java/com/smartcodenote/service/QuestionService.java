package com.smartcodenote.service;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.dto.GenerateQuestionRequest;
import com.smartcodenote.dto.QuestionDetailResponse;
import com.smartcodenote.dto.QuestionListItemResponse;
import com.smartcodenote.dto.QuestionQueryRequest;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface QuestionService {

    List<QuestionDetailResponse> generate(Long userId, GenerateQuestionRequest request);

    PageResponse<QuestionListItemResponse> list(Long userId, QuestionQueryRequest request);

    QuestionDetailResponse detail(Long userId, Long questionId);

    void delete(Long userId, Long questionId);

    void generateStream(Long userId, GenerateQuestionRequest request, SseEmitter emitter);
}
