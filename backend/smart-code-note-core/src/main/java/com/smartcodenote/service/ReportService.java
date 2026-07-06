package com.smartcodenote.service;

import com.smartcodenote.dto.LearningOverviewResponse;
import com.smartcodenote.dto.LearningSuggestionResponse;
import com.smartcodenote.dto.WeakKnowledgeResponse;
import java.util.List;

public interface ReportService {

    LearningOverviewResponse overview(Long userId);

    List<WeakKnowledgeResponse> weakKnowledge(Long userId, int limit);

    LearningSuggestionResponse suggestions(Long userId);
}
