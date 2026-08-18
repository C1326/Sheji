package com.sheji.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime createTime;
}
