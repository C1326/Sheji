package com.sheji.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DiaryDTO {
    private Long diaryId;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    /** 0 私有；1 公开 */
    private Integer permission = 1;
    /** 媒体文件相对路径列表，如 /uploads/xxx.jpg */
    private List<String> mediaUrls;
}
