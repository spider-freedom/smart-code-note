package com.smartcodenote.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningSuggestionResponse {

    private String summary;
    private List<String> suggestions;
    private List<WeakKnowledgeResponse> weakKnowledgePoints;
}
