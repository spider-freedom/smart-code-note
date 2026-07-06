package com.smartcodenote.service;

import com.smartcodenote.common.PageResponse;
import com.smartcodenote.dto.GenerateKnowledgeRequest;
import com.smartcodenote.dto.KnowledgeDetailResponse;
import com.smartcodenote.dto.KnowledgeListItemResponse;
import com.smartcodenote.dto.KnowledgeQueryRequest;
import com.smartcodenote.dto.UpdateKnowledgeRequest;
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
