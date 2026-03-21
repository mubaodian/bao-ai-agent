package com.swl.baoaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是木宝典";
        //第一轮
        String response = loveApp.doChat(message,chatId);
        assertNotNull(response);
        //第二轮
        message = "我的对象是张三";
        response = loveApp.doChat(message,chatId);
        assertNotNull(response);
        //第三轮
        message = "我是谁，我的对象又是谁，我刚刚跟你说过了，你还记得吗";
        response = loveApp.doChat(message,chatId);
        assertNotNull(response);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是木宝典,，我想让另一半（张三）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message,chatId);
        assertNotNull(loveReport);
    }
}