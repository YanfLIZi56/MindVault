package com.yanglizi.aigc.constants;

public class GenStatusContants {
    public static final int PENDING = 1;      // 排队待执行
    public static final int GENERATING = 2;   // 生成中（大模型正在输出token）
    public static final int SUCCESS = 3;      // 生成完成
    public static final int FAILED = 4;       // 生成失败
    public static final int CANCELLED = 5;     // 用户主动取消任务
    public static final int MANUAL = 6;       // 手动处理
}
