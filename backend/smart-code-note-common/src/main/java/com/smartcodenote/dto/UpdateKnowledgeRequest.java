package com.smartcodenote.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UpdateKnowledgeRequest {

    @NotBlank(message = "title is required")
    @Size(max = 128, message = "title length must be less than or equal to 128")
    private String title;

    @Size(max = 32, message = "type length must be less than or equal to 32")
    private String type;

    private String summary;

    @Size(max = 32, message = "difficulty length must be less than or equal to 32")
    private String difficulty;

    @Min(value = 0, message = "masteryLevel must be greater than or equal to 0")
    @Max(value = 5, message = "masteryLevel must be less than or equal to 5")
    private Integer masteryLevel;

    private LocalDateTime nextReviewTime;
}
