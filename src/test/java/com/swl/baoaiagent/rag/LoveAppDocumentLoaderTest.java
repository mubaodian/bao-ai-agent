package com.swl.baoaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppDocumentLoaderTest {

    @Resource
    private LoveAppDocumentLoader loader;

    @Test
    void loaderMarkdowns() {
        List<Document> documents = loader.loaderMarkdowns();
        assertNotNull(documents);
    }
}