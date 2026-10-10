package com.swl.baoaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring AI 框架调用AI大模型(阿里)
 */
//@Component
@Deprecated
public class SpringAiAiInvoke implements CommandLineRunner {
    @Resource //该注解是优先以名称匹配Bean，若未找到则按类型匹配
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String...args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好,我正在学习Spring AI 框架"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
