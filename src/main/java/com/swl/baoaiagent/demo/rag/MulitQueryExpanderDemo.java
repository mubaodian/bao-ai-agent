package com.swl.baoaiagent.demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询扩展器 Demo
 */
@Component
public class MulitQueryExpanderDemo {
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 构造函数
     * @param dashscopeChatModel
     */
    public MulitQueryExpanderDemo(ChatModel dashscopeChatModel){
        this.chatClientBuilder = ChatClient.builder(dashscopeChatModel);
    }

    /**
     * 多查询扩展
     * @param query
     * @return
     */
    public List<Query> expand(String query){
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query(query));
        return queries;
    }
}
