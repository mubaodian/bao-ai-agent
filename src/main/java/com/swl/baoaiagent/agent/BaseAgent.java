package com.swl.baoaiagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程
 *
 * 提供状态转换，内存管理和基于step步骤的执行循环的基础功能
 */
@Data
@Slf4j
public abstract class BaseAgent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 代理名称
    private String name;

    //提示词
    private String systemPrompt;
    private String nextStepPrompt;

    //代理状态
    private AgentState state = AgentState.IDLE;

    //执行步骤控制
    private int currentStep = 0;
    private int maxStep = 10;

    //LLM大模型
    private ChatClient chatClient;

    //Memory 记忆（需要自主维护会话上下文）
    List<Message> messageList = new ArrayList<>();

    //结构化事件的接收器（仅流式路径安装；为空时 emit 自动 no-op）
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private transient Consumer<String> eventSink;

    //最终回答（由 think() 记录，循环结束后统一发送一次）
    private String finalAnswer;

    //客户端是否已断开（emit 失败时置位）
    private volatile boolean clientDisconnected;

    /**
     * 将单个事件序列化为单行 JSON 后推送给前端。
     * sink 为空（同步 run() 或 Love 路径）时不产生任何副作用。
     */
    protected void emit(AgentEvent event) {
        if (eventSink == null) {
            return;
        }
        try {
            eventSink.accept(MAPPER.writeValueAsString(event));
        } catch (IOException | RuntimeException e) {
            // 不得向上抛：否则会被 think() 的 catch(Exception) 吞掉并破坏循环控制流
            log.warn("推送事件失败：{}", e.getMessage());
            clientDisconnected = true;
        }
    }

    protected void emit(String type, String tool, String label, String text, String detail) {
        emit(new AgentEvent(type, currentStep, tool, label, text, detail, null));
    }

    /**
     * 运行代理（智能体）
     * @param userPrompt 用户输入的提示词
     * @return 执行结果
     */
    public String run (String userPrompt){
        // 1.基础校验
        if(this.state != AgentState.IDLE){
            throw new RuntimeException("Cannot run agent from state:" + this.state);
        }
        if(StringUtil.isEmpty(userPrompt)){
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        // 2.执行、更改状态
        this.state = AgentState.RUNNING;

        //记录上下文
        this.messageList.add(new UserMessage(userPrompt));

        // Agent Loop 执行循环
        List<String> results = new ArrayList<>(); //保存结果列表
        try {
            for(int i = 0;i < this.maxStep && this.state != AgentState.FINISHED;i++){
                int stepNumber = i + 1;
                this.currentStep = stepNumber;
                log.info("当前执行步骤为：{}/{}",stepNumber,this.maxStep);
                //单次执行
                String stepResult = step();
                String result = "Step " + stepNumber + ":" + stepResult;
                results.add(result);
            }
            // 循环结束，状态仍未完成，且达到最大步骤数，强制标记为完成状态
            if(this.currentStep >= this.maxStep){
                this.state = AgentState.FINISHED;
                results.add(("Terminated: Reached max steps (" + this.maxStep + ")"));
            }
            return String.join("\n",results);
        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3.清理资源
            cleanup();
        }
    }

    /**
     * 运行代理（智能体）,流式输出
     * @param userPrompt 用户输入的提示词
     * @return 执行结果
     */
    public SseEmitter runWithStream (String userPrompt){
        SseEmitter sseEmitter = new SseEmitter(300000L);
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() ->{
            // 安装事件接收器：仅流式路径安装，同步 run() 保持 no-op
            setEventSink(json -> {
                try {
                    sseEmitter.send(json);
                } catch (IOException ex) {
                    setClientDisconnected(true);
                }
            });
            setFinalAnswer(null);
            setClientDisconnected(false);

            // 1.基础校验
            if(this.state != AgentState.IDLE){
                emit("error", null, null, "无法从状态运行代理：" + this.state, null);
                sseEmitter.complete();
                return;
            }
            if(StringUtil.isEmpty(userPrompt)){
                emit("error", null, null, "不能使用空提示词运行代理", null);
                sseEmitter.complete();
                return;
            }
            // 2.执行、更改状态
            this.state = AgentState.RUNNING;

            //记录上下文
            this.messageList.add(new UserMessage(userPrompt));

            // Agent Loop 执行循环
            try {
                for(int i = 0;i < this.maxStep && this.state != AgentState.FINISHED && !clientDisconnected;i++){
                    int stepNumber = i + 1;
                    this.currentStep = stepNumber;
                    log.info("当前执行步骤为：{}/{}",stepNumber,this.maxStep);
                    //单次执行（过程事件由 think()/act() 内部发出）
                    step();
                }
                // 循环结束，状态仍未完成，且达到最大步骤数，强制标记为完成状态
                if(this.currentStep >= this.maxStep){
                    this.state = AgentState.FINISHED;
                }
                if(!clientDisconnected){
                    if(this.finalAnswer != null && !this.finalAnswer.isBlank()){
                        emit(new AgentEvent("answer", currentStep, null, null, finalAnswer, null, null));
                    } else if(this.currentStep >= this.maxStep){
                        emit(new AgentEvent("notice", currentStep, null, null,
                                "已达到最大步数(" + this.maxStep + ")", null, null));
                    }
                    emit(new AgentEvent("done", currentStep, null, null, null, null, null));
                }
                sseEmitter.complete();
            } catch (Exception e) {
                this.state = AgentState.ERROR;
                log.error("Error executing agent", e);
                emit("error", null, null, "执行错误：" + e.getMessage(), null);
                try {
                    sseEmitter.complete();
                } catch (Exception ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3.清理资源
                setEventSink(null);
                cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if(this.state == AgentState.RUNNING){
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * 定义单个执行步骤方法，用于代理执行具体任务
     * @return 执行结果
     */
    public abstract String step();

    /**
     * 清理代理使用的系统资源，如关闭连接、释放内存等
     */
    public void cleanup(){}
}
