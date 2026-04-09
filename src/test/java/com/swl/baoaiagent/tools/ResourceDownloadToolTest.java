package com.swl.baoaiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://java2ai.com/integration/toolcalls/tool-calls#%E6%94%AF%E6%8C%81%E7%9A%84%E6%89%A9%E5%B1%95%E5%AE%9E%E7%8E%B0";
        String fileName = "test.txt";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }
}