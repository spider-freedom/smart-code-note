package com.smartcodenote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateKnowledgeRequest {

    @NotNull(message = "noteId is required")
    private Long noteId;
}
