package com.sheji.controller.vo;

import lombok.Data;

/**
 * 文件上传结果：返回给前端保存，提交日记时把 filePath 列表放到 mediaUrls 一起提交。
 */
@Data
public class UploadVO {
    /** 相对访问路径，如 /uploads/xxx.jpg */
    private String filePath;
    private String fileName;
    private Long fileSize;
    /** 1 图片；2 视频 */
    private Integer mediaType;
}
