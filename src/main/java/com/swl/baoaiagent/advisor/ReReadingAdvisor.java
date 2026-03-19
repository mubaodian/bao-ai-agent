package com.swl.baoaiagent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义Re2 Advisor
 * 可以提高大语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    //执行请求前，构造一个新的AdvisedRequest，改写Prompt
    private AdvisedRequest before(AdvisedRequest advisedRequest){
        Map<String,Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query",advisedRequest.userText());
        return AdvisedRequest.from(advisedRequest)
                .userText(
                        """
                               {re2_input_query}
                               Read the question again:{re2_input_query}
                                """
                )
                .userParams(advisedUserParams)
                .build();

    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        advisedRequest = before(advisedRequest);
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = before(advisedRequest);
        return chain.nextAroundStream(advisedRequest);
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
