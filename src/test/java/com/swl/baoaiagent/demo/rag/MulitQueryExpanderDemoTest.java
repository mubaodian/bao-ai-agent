package com.swl.baoaiagent.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MulitQueryExpanderDemoTest {
    @Resource
    private MulitQueryExpanderDemo mulitQueryExpanderDemo;

    @Test
    void expand() {
        List<Query> queries = mulitQueryExpanderDemo.expand("哇哈哈哈哈哈程序员是干嘛的啊，告诉我这个小白吧嘻嘻嘻嘻哈哈哈");
        Assertions.assertNotNull(queries);
    }
}