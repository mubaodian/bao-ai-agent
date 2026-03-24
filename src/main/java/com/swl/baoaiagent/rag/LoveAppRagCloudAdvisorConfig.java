package com.swl.baoaiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义基于阿里云知识库服务的RAG增强顾问
 */
@Configuration
public class LoveAppRagCloudAdvisorConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
    Advisor loveAppRagCloudAdvisor(){
        DashScopeApi dashScopeApi = new DashScopeApi(dashScopeApiKey);
        //知识库索引名称
        final String KNOWLEDGE_INDEX = "恋爱大师";
        //文档检索器
        DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build());
        //创建顾问
       return RetrievalAugmentationAdvisor.builder()
               .documentRetriever(dashScopeDocumentRetriever)
               .build();

    }
}
