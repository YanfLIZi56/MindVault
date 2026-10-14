package com.yanglizi.common.pojo.dto;

import lombok.Data;

@Data
public class PageDto {
    private int pageNum;
    private int pageSize;
    private String sort;
}
