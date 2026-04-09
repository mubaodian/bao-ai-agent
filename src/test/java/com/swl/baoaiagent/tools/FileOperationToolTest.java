package com.swl.baoaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileOperationToolTest {
    private FileOperationTool fileTool = new FileOperationTool();

    @Test
    void readFile() {
        String fileName = "学习AI编程.txt";
        String result = fileTool.readFile(fileName);
        assertNotNull(result);
    }

    @Test
    void writeFile() {
        String fileName = "学习AI编程.txt";
        String content = "学AI，用AI";
        String result = fileTool.writeFile(content, fileName);
        assertNotNull(result);
    }
}