package com.itheima.smartcodenote.rag;

import com.itheima.smartcodenote.dto.ChatLearningContext;
import com.itheima.smartcodenote.entity.Note;
import com.itheima.smartcodenote.entity.NoteChunk;
import com.itheima.smartcodenote.mapper.NoteMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds RAG-augmented system prompt by combining retrieved note chunks
 * with the user's learning context.
 */
@Component
public class RagContextBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final NoteMapper noteMapper;

    public RagContextBuilder(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    /**
     * Build the RAG context section for the system prompt.
     *
     * @param scoredChunks    retrieved note chunks with similarity scores
     * @param learningContext user's learning statistics
     * @return formatted context string ready to be appended to system prompt
     */
    public String buildContext(List<RetrievalService.ScoredChunk> scoredChunks,
                                ChatLearningContext learningContext) {
        StringBuilder sb = new StringBuilder();

        // ── Reference materials section ──
        if (scoredChunks != null && !scoredChunks.isEmpty()) {
            sb.append("[参考学习资料]\n");

            // Group chunks by note
            Map<Long, List<RetrievalService.ScoredChunk>> groupedByNote = scoredChunks.stream()
                    .collect(Collectors.groupingBy(
                            sc -> sc.chunk().getNoteId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            for (Map.Entry<Long, List<RetrievalService.ScoredChunk>> entry : groupedByNote.entrySet()) {
                Note note = noteMapper.selectById(entry.getKey());
                String noteTitle = note != null ? note.getTitle() : "未知笔记";
                String noteDate = "";
                if (note != null && note.getCreateTime() != null) {
                    noteDate = " (" + note.getCreateTime().format(DATE_FORMAT) + " 上传)";
                }

                sb.append("--- 笔记「").append(noteTitle).append("」").append(noteDate).append(" ---\n");

                for (RetrievalService.ScoredChunk sc : entry.getValue()) {
                    sb.append(sc.chunk().getContent()).append("\n\n");
                }
            }
        }

        // ── Learning state section ──
        sb.append("[学习状态]\n");
        if (learningContext.getRecentNoteTitle() != null) {
            sb.append("- 最近在学: ").append(learningContext.getRecentNoteTitle()).append("\n");
        }
        sb.append("- 知识掌握: 已掌握 ").append(learningContext.getMasteredKnowledgePoints())
                .append(" 个知识点，共 ").append(learningContext.getTotalKnowledgePoints()).append(" 个\n");

        if (learningContext.getReviewDueCount() > 0) {
            sb.append("- 待复习: ").append(learningContext.getReviewDueCount()).append(" 个知识点\n");
        }

        if (learningContext.getTodayPracticeCount() > 0) {
            sb.append("- 今日练习: 完成 ").append(learningContext.getTodayPracticeCount())
                    .append(" 题，正确率 ").append(learningContext.getTodayCorrectRate()).append("%\n");
        } else {
            sb.append("- 今日尚未开始练习\n");
        }

        return sb.toString();
    }

    /**
     * Build the RAG instruction appended to the system prompt.
     */
    public String buildInstruction() {
        return """
                请优先基于[参考学习资料]中的内容回答用户问题。
                如果参考资料中包含相关信息，请引用具体内容。
                如果参考资料中没有相关信息，再结合你的技术知识回答，并说明"这部分内容在你的笔记中暂时没有找到"。
                回答要简洁、温暖，像好朋友之间的对话。控制在150字以内。""";
    }
}
