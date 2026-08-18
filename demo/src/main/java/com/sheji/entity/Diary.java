package com.sheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diary")
public class Diary {
    @TableId(type = IdType.AUTO)
    private Long diaryId;
    private Long userId;
    private String title;
    private String content;
    /** 0 私有；1 公开 */
    private Integer permission;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
