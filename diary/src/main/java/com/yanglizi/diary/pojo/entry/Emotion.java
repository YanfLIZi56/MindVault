package com.yanglizi.diary.pojo.entry;

import lombok.Data;

@Data
public class Emotion {

    private Integer id;
    private String name;
    private String tag;
    private String iconUrl;
    private Integer sortOrder;
    private String createdAt;
    private Integer aiGen;
}
