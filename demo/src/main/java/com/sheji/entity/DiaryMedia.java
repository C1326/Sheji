package com.sheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diary_media")
public class DiaryMedia {
    @TableId(type = IdType.AUTO)
    private Long mediaId;
    private Long diaryId;
    /** 1 图片；2 视频 */
    private Integer mediaType;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private LocalDateTime createTime;
}
