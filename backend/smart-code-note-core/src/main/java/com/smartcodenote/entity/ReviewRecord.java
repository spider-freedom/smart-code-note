package com.smartcodenote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_record")
public class ReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long knowledgeId;
    private Long questionId;
    private String reviewResult;
    private Integer score;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createTime;
}
