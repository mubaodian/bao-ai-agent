package com.swl.baoaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;
    @Resource
    private MyTokenSplitter myTokenSplitter;
    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        //基于内存的向量数据库
        SimpleVectorStore loveAppVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        List<Document> documents = loveAppDocumentLoader.loaderMarkdowns();
        //基于token的文本切分器
        List<Document> splitDocuments = myTokenSplitter.splitCustomized(documents);
        //为文档补充元信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocument(splitDocuments);
        loveAppVectorStore.doAdd(enrichedDocuments);
        return loveAppVectorStore;
    }
}
