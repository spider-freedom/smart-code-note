package com.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTaskResponse {

    private Long knowledgeId;
    private Long noteId;
    private String title;
    private String type;
    private String summary;
    private String difficulty;
    private Integer masteryLevel;
    private LocalDateTime nextReviewTime;
}
