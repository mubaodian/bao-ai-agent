package com.swl.baoaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

/**
 * 创建上下文查询增强器的工厂
 */
public class LoveAppContextualQueryAugumneterFactory {

    public static QueryAugmenter createInstance(){
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                 你应该输出下面的内容：
                                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                                有问题可以联系我们的客服：https://www.baidu.com
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
