package com.swl.baoaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import com.swl.baoaiagent.advisor.MyLoggerAdvisor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent{
    //可用工具
    private final ToolCallback[] availableTools;

    //保存工具调用信息的响应结果（要调用哪些工具）
    private ChatResponse toolCallChatResponse;

    //工具调用管理者
    private final ToolCallingManager toolCallingManager;

    //禁用Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools){
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // true表示禁用Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    @Override
    public boolean think() {
        // 1.校验提示词，拼接用户提示词
        if(getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()){
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2.调用AI大模型，获取工具调用结果
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(this.availableTools)
                    .call()
                    .chatResponse();
            this.toolCallChatResponse = chatResponse;
            // 获取助手消息assistantMessage
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取助手消息的思考内容
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            // 3.解析工具调用结果，获取要调用的工具
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            // 判断是否需要使用工具
            if(toolCalls.isEmpty()){
                getMessageList().add(assistantMessage); //为false时才需要更新列表，为true时act()方法会自动更新
                return false;
            }
            // 需要使用工具,返回true，执行act()
            log.info(getName() + "选择了" + toolCalls.size() + "个工具来使用");
            String toolCallInfo = toolCalls.stream()
                    .map(toolCall -> String.format("工具名称：%s，工具参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(getName() + "调用的工具信息：" + toolCallInfo);
            return true;
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题:" + e.getMessage());
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if(!toolCallChatResponse.hasToolCalls()){
            return "没有工具调用";
        }

        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, this.toolCallChatResponse);
        //更新消息列表，自动将先前的消息、助手消息、工具调用消息都添加进去了
        setMessageList(toolExecutionResult.conversationHistory());

        // 输出调用工具结果信息
        ToolResponseMessage toolResponseMessage= (ToolResponseMessage)CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具" + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));

        // 判断是否使用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if(terminateToolCalled){
            setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;
    }
}
