package com.smartcodenote.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatLearningContext {

    private int totalNotes;
    private int totalKnowledgePoints;
    private int masteredKnowledgePoints;
    private int reviewDueCount;
    private int todayPracticeCount;
    private double todayCorrectRate;
    private String recentNoteTitle;
}
