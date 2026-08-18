package com.sheji.controller.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageResultVO<T> {
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer pageSize;
}
