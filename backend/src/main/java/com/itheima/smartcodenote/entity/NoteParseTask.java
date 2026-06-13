package com.itheima.smartcodenote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Tracks async AI knowledge/ question generation tasks.
 * One task per note — created when note is uploaded, updated as AI processing progresses.
 */
@Data
@TableName("note_parse_task")
public class NoteParseTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long noteId;

    /** PENDING | PROCESSING | COMPLETED | FAILED */
    private String status;
    private String errorMessage;

    /** Number of knowledge points generated */
    private Integer knowledgeCount;

    /** Number of questions generated */
    private Integer questionCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
