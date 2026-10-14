package com.yanglizi.aigc.constants;

public class ChatConstant {

    // 对话中
    public static final Integer CHATTING = 1;

    // 已生成日记
    public static final Integer FINISHED = 2;

    // 已放弃(超时/关闭)
    public static final Integer ABANDONED = 3;

    public static final Integer USER_SPEAKER = 1;

    public static final Integer AI_SPEAKER = 2;

    public static final String ROLE_USER = "USER";
    public static final String ROLE_AI = "AI";

    public static final String SESSION_KEY = "chat:session:";

    public static final String MEMORY_KEY = "chat:memory:";
}
