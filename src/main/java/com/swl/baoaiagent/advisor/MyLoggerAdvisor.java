package com.swl.baoaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

/**
 * 自定义日志Advisor
 * 打印info级别日志，只输出单词用户提示词和AI回复的文本内容
 */
@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 打印用户提示词
    private void before(AdvisedRequest advisedRequest){
        log.info("Ai Request:{}",advisedRequest.userText());
    }
    // 打印AI回复
    private void observeAfter(AdvisedResponse advisedResponse){
        log.info("Ai Response:{}",advisedResponse.response().getResult().getOutput().getText());
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        observeAfter(advisedResponse);
        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        before(advisedRequest);
        Flux<AdvisedResponse> adviseResponses = chain.nextAroundStream(advisedRequest);
        return new MessageAggregator().aggregateAdvisedResponse(adviseResponses,this::observeAfter);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
