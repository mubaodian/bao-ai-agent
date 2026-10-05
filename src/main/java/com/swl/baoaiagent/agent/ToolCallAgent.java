package com.swl.baoaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
    //工具返回内容的详情上限（超出截断，避免超大 HTML/JSON 撑爆事件帧）
    private static final int DETAIL_LIMIT = 8000;
    //摘要单行文本的最大长度
    private static final int SUMMARY_LIMIT = 200;

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
                // 记录最终回答，循环结束后统一发送一次（think 返回 false 不会终止循环）
                if(result != null && !result.isBlank()){
                    setFinalAnswer(result);
                }
                getMessageList().add(assistantMessage); //为false时才需要更新列表，为true时act()方法会自动更新
                return false;
            }
            // 需要使用工具,返回true，执行act()
            log.info(getName() + "选择了" + toolCalls.size() + "个工具来使用");
            String toolCallInfo = toolCalls.stream()
                    .map(toolCall -> String.format("工具名称：%s，工具参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(getName() + "调用的工具信息：" + toolCallInfo);
            // 逐个下发工具调用事件（doTerminate 不发）
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                if ("doTerminate".equals(toolCall.name())) {
                    continue;
                }
                String args = toolCall.arguments();
                emit(new AgentEvent("tool_call", getCurrentStep(), toolCall.name(), labelFor(toolCall.name()),
                        describeCall(toolCall.name(), args), truncate(args), exceedsLimit(args)));
            }
            return true;
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题:" + e.getMessage());
            emit("error", null, null, "思考过程出现问题：" + e.getMessage(), null);
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
        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            // doTerminate 的结果不作为工具结果下发
            if ("doTerminate".equals(response.name())) {
                continue;
            }
            String raw = response.responseData();
            String detail = truncate(raw);
            emit(new AgentEvent("tool_result", getCurrentStep(), response.name(), labelFor(response.name()),
                    summarize(raw), detail, exceedsLimit(raw)));
        }
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

    /**
     * 工具名称 -> 可读中文标签
     */
    private static String labelFor(String tool) {
        if (tool == null) {
            return "工具";
        }
        return switch (tool) {
            case "searchWeb" -> "联网搜索";
            case "scrapeWebPage" -> "网页抓取";
            case "readFile" -> "读取文件";
            case "writeFile" -> "写入文件";
            case "downloadResource" -> "下载资源";
            case "executeTerminalCommand" -> "执行命令";
            case "generatePDF" -> "生成 PDF";
            default -> tool;
        };
    }

    /**
     * 依据工具与参数生成单行调用描述
     */
    private static String describeCall(String tool, String argsJson) {
        if (tool == null) {
            return "正在调用工具";
        }
        String value = switch (tool) {
            case "searchWeb" -> argValue(argsJson, "query");
            case "scrapeWebPage" -> argValue(argsJson, "url");
            case "readFile", "writeFile", "generatePDF" -> argValue(argsJson, "fileName");
            case "downloadResource" -> argValue(argsJson, "url", "fileName");
            case "executeTerminalCommand" -> argValue(argsJson, "command");
            default -> null;
        };
        if (value == null || value.isBlank()) {
            return switch (tool) {
                case "searchWeb" -> "正在搜索";
                case "scrapeWebPage" -> "正在读取网页";
                case "readFile" -> "正在读取文件";
                case "writeFile" -> "正在写入文件";
                case "downloadResource" -> "正在下载资源";
                case "executeTerminalCommand" -> "正在执行命令";
                case "generatePDF" -> "正在生成 PDF";
                default -> "正在调用工具：" + tool;
            };
        }
        String oneLine = summarize(value);
        return switch (tool) {
            case "searchWeb" -> "正在搜索：" + oneLine;
            case "scrapeWebPage" -> "正在读取网页：" + oneLine;
            case "readFile" -> "正在读取文件：" + oneLine;
            case "writeFile" -> "正在写入文件：" + oneLine;
            case "downloadResource" -> "正在下载资源：" + oneLine;
            case "executeTerminalCommand" -> "正在执行命令：" + oneLine;
            case "generatePDF" -> "正在生成 PDF：" + oneLine;
            default -> "正在调用工具：" + tool;
        };
    }

    /**
     * 从参数 JSON 中按顺序取第一个非空的键值
     */
    private static String argValue(String argsJson, String... keys) {
        if (argsJson == null || argsJson.isBlank()) {
            return null;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(argsJson);
            for (String key : keys) {
                Object value = obj.get(key);
                if (value != null && !String.valueOf(value).isBlank()) {
                    return String.valueOf(value);
                }
            }
        } catch (Exception ignored) {
            // 参数不是标准 JSON 时忽略，回退到通用描述
        }
        return null;
    }

    /**
     * 折叠空白为单行短摘要
     */
    private static String summarize(String raw) {
        if (raw == null) {
            return "";
        }
        String oneLine = raw.replaceAll("\\s+", " ").trim();
        return oneLine.length() > SUMMARY_LIMIT ? oneLine.substring(0, SUMMARY_LIMIT) + "…" : oneLine;
    }

    /**
     * 截断详情到上限
     */
    private static String truncate(String raw) {
        if (raw == null || raw.length() <= DETAIL_LIMIT) {
            return raw;
        }
        return raw.substring(0, DETAIL_LIMIT);
    }

    private static boolean exceedsLimit(String raw) {
        return raw != null && raw.length() > DETAIL_LIMIT;
    }
}
