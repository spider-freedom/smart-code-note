package com.itheima.smartcodenote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String category;
    private String tags;
    private String fileUrl;
    private String fileType;
    private String originalContent;
    private String cleanContent;
    private Integer parseStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
