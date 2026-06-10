package com.itheima.smartcodenote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("note_chunk")
public class NoteChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long noteId;
    private Long knowledgeId;
    private Integer chunkIndex;
    private String content;
    private byte[] embedding;
    private Integer tokenCount;
    private LocalDateTime createTime;
}
