package com.swl.baoaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;

public class LangChainAiInvoke {
    public static void main(String[] args) {
        ChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();

        String response = qwenChatModel.chat("你好，我是一名大二学生，现在正在学习Ai");
        System.out.println(response);
    }
}
