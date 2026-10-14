package com.yanglizi.aigc.enums;

public enum Intent {
    START_DIARY("用户想开始写日记"),
    CONTINUE_DIARY("用户正在提供日记内容/细节"),
    CONFIRM_GENERATE("用户确认生成日记（如'好了'、'可以了'、'生成吧'）"),
    CHAT("普通闲聊（打招呼、问好、日常聊天）"),
    HELP("询问功能（'你能做什么'、'怎么用'）"),
    OFF_TOPIC("明显偏题，跟你聊与日记/聊天无关（如'帮我写代码'、'今天股票怎么样'）"),
    UNKNOWN("无法判断");

    private final String description;

    Intent(String description) {
        this.description = description;
    }

    /**
     * 获取所有意图的描述文本（用于生成提示词）
     */
    public static String getAllDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (Intent intent : values()) {
            sb.append("- ")
                    .append(intent.name())
                    .append(": ")
                    .append(intent.description)
                    .append("\n");
        }
        return sb.toString();
    }
}