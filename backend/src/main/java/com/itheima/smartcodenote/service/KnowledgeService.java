package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.dto.GenerateKnowledgeRequest;
import com.itheima.smartcodenote.dto.KnowledgeDetailResponse;
import com.itheima.smartcodenote.dto.KnowledgeListItemResponse;
import com.itheima.smartcodenote.dto.KnowledgeQueryRequest;
import com.itheima.smartcodenote.dto.UpdateKnowledgeRequest;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface KnowledgeService {

    PageResponse<KnowledgeListItemResponse> list(Long userId, KnowledgeQueryRequest request);

    KnowledgeDetailResponse detail(Long userId, Long knowledgeId);

    KnowledgeDetailResponse update(Long userId, Long knowledgeId, UpdateKnowledgeRequest request);

    void delete(Long userId, Long knowledgeId);

    int batchDelete(Long userId, List<Long> knowledgeIds);

    List<KnowledgeDetailResponse> generate(Long userId, GenerateKnowledgeRequest request);

    void generateStream(Long userId, GenerateKnowledgeRequest request, SseEmitter emitter);
}
