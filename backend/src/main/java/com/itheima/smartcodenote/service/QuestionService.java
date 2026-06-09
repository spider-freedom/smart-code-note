package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.dto.GenerateQuestionRequest;
import com.itheima.smartcodenote.dto.QuestionDetailResponse;
import com.itheima.smartcodenote.dto.QuestionListItemResponse;
import com.itheima.smartcodenote.dto.QuestionQueryRequest;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface QuestionService {

    List<QuestionDetailResponse> generate(Long userId, GenerateQuestionRequest request);

    PageResponse<QuestionListItemResponse> list(Long userId, QuestionQueryRequest request);

    QuestionDetailResponse detail(Long userId, Long questionId);

    void delete(Long userId, Long questionId);

    void generateStream(Long userId, GenerateQuestionRequest request, SseEmitter emitter);
}
