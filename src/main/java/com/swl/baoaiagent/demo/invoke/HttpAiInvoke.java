package com.swl.baoaiagent.demo.invoke;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.Map;

public class HttpAiInvoke {

    public static void main(String[] args) {
        // 设置 API 密钥
        String apiKey = System.getenv("DB_TEST_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("错误：DASHSCOPE_API_KEY 环境变量未设置");
            return;
        }

        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        // 构建请求体
        JSONObject messages = new JSONObject();
        messages.put("role", "system");
        messages.put("content", "You are a helpful assistant.");

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "你是谁？");

        JSONObject input = new JSONObject();
        input.put("messages", JSONUtil.createArray().put(messages).put(userMessage));

        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");

        JSONObject body = new JSONObject();
        body.put("model", "qwen-plus");
        body.put("input", input);
        body.put("parameters", parameters);

        // 发送 POST 请求
        String response = HttpUtil.createPost("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
                .addHeaders(headers)
                .body(body.toString())
                .execute()
                .body();

        // 处理响应
        System.out.println(response);
    }
}
