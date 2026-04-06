package com.swl.baoaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoveAppDocumentLoader {

    //批量解析资源
    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 读取多篇Markdown文件，获取文档列表
     */
    public List<Document> loaderMarkdowns(){
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 读取document目录下的所有.md文件，解析为Resource对象数组
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            // 遍历Resource数组，解析每个文件
            for(Resource r : resources){
                String filename = r.getFilename();
                //给每个文档加上status元数据
                String status = filename.substring(filename.length() - 6, filename.length() - 4);
                //文档的config配置
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("status",status)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(r, config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("读取Markdown文件失败", e);
        }
        return allDocuments;
    }

}
