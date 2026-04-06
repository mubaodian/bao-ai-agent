package com.swl.baoaiagent.rag;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 查询重写器
 */
@Component
public class QueryRewriter {
    private final QueryTransformer queryTransformer;

    public QueryRewriter(ChatModel dashscopeChatModel){
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);

        this.queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 查询重写
     * @param prompt
     * @return
     */
    public String doQueryRewrite(String prompt){
        Query query = new Query(prompt);

        Query transformedQuery = queryTransformer.transform(query);

        return transformedQuery.text();
    }

}
