package com.swl.baoaiagent.agent;

import lombok.Data;
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

/**
 * 抽象基础代理类，用于管理代理状态和执行流程
 *
 * 提供状态转换，内存管理和基于step步骤的执行循环的基础功能
 */
@Data
@Slf4j
public abstract class BaseAgent {

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
            // 1.基础校验
            try {
                if(this.state != AgentState.IDLE){
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if(StringUtil.isEmpty(userPrompt)){
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
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
                    //输出当前每一步的结果到 SSE
                    sseEmitter.send(result);
                }
                // 循环结束，状态仍未完成，且达到最大步骤数，强制标记为完成状态
                if(this.currentStep >= this.maxStep){
                    this.state = AgentState.FINISHED;
                    results.add(("Terminated: Reached max steps (" + this.maxStep + ")"));
                    sseEmitter.send("Terminated: Reached max steps (" + this.maxStep + ")");
                }
                sseEmitter.complete();
            } catch (Exception e) {
                this.state = AgentState.ERROR;
                log.error("Error executing agent", e);
                try {
                    sseEmitter.send("执行错误" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3.清理资源
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
