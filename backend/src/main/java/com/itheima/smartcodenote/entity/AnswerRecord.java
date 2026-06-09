package com.itheima.smartcodenote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("answer_record")
public class AnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    private Integer score;

    @TableField("is_correct")
    private Integer correct;

    private String aiComment;
    private LocalDateTime createTime;
}
