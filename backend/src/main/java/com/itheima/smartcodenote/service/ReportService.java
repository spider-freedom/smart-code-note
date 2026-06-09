package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.dto.LearningOverviewResponse;
import com.itheima.smartcodenote.dto.LearningSuggestionResponse;
import com.itheima.smartcodenote.dto.WeakKnowledgeResponse;
import java.util.List;

public interface ReportService {

    LearningOverviewResponse overview(Long userId);

    List<WeakKnowledgeResponse> weakKnowledge(Long userId, int limit);

    LearningSuggestionResponse suggestions(Long userId);
}
