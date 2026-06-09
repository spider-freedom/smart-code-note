package com.itheima.smartcodenote.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDetailResponse {

    private Long id;
    private Long noteId;
    private String title;
    private String type;
    private String summary;
    private String difficulty;
    private Integer masteryLevel;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
