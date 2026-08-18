package com.sheji.controller.vo;

import lombok.Data;

@Data
public class MediaVO {
    private Long mediaId;
    /** 1 图片；2 视频 */
    private Integer mediaType;
    private String url;
    private String fileName;
    private Long fileSize;
}
