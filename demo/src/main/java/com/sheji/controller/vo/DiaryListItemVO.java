package com.sheji.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiaryListItemVO {
    private Long diaryId;
    private Long userId;
    private String title;
    private String content;
    private Integer permission;
    private LocalDateTime createTime;
    private String nickname;
    private String avatar;
    private List<MediaVO> mediaList;
}
