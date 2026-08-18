package com.sheji.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiaryDetailVO {
    private Long diaryId;
    private Long userId;
    private String title;
    private String content;
    private Integer permission;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String nickname;
    private String avatar;
    private List<MediaVO> mediaList;
    /** 当前用户是否可下载（公开日记或本人日记） */
    private Boolean canDownload;
}
