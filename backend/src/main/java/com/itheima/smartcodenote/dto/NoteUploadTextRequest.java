package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoteUploadTextRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String category;

    private String tags;
}
