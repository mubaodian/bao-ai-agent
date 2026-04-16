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
        String response = loveApp.doChat(message, chatId);
        assertNotNull(response);
        //第二轮
        message = "我的对象是张三";
        response = loveApp.doChat(message, chatId);
        assertNotNull(response);
        //第三轮
        message = "我是谁，我的对象又是谁，我刚刚跟你说过了，你还记得吗";
        response = loveApp.doChat(message, chatId);
        assertNotNull(response);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是木宝典,，我想让另一半（张三）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办";
        String response = loveApp.doChatWithRag(message, chatId);
        assertNotNull(response);
    }

    @Test
    void doChatWithTool() {
        //联网测试
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");
        //网页抓取测试
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");
        //资源下载测试
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");
        //终端操作测试
        testMessage("执行 Python3 脚本来生成数据分析报告");
        //文件操作测试
        testMessage("保存我的恋爱档案为文件");
        //PDF生成测试
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String response = loveApp.doChatWithTool(message, chatId);
        assertNotNull(response);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
//        String message = "我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点";
        String  message = "帮我搜索一些哄另一半开心的图片";
        String response = loveApp.doChatWithMcp(message, chatId);
        assertNotNull(response);
    }
}