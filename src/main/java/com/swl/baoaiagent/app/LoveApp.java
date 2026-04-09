package com.swl.baoaiagent.app;

import com.swl.baoaiagent.advisor.MyLoggerAdvisor;
import com.swl.baoaiagent.advisor.ReReadingAdvisor;
import com.swl.baoaiagent.chatmemory.FileBasedChatMemory;
import com.swl.baoaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.swl.baoaiagent.rag.QueryRewriter;
import com.swl.baoaiagent.tools.ToolRegistration;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {
    //对话客户端
    private ChatClient chatClient;
    //系统预设
    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
    //Rag 内存向量数据库
    @Resource
    private VectorStore loveAppVectorStore;

    //Rag 顾问
    @Resource
    private Advisor loveAppRagCloudAdvisor;

    //Rag PgVector向量数据库
//    @Resource
//    private VectorStore pgVectorVectorStore;

    //Rag 查询重写器
    @Resource
    private QueryRewriter queryRewriter;

    //工具注册类
    @Resource
    private ToolRegistration toolRegistration;

    /**
     * 构造函数
     * 初始化对话客户端
     * @param deshscopeChatModel
     */
    public LoveApp(ChatModel deshscopeChatModel) {
       /* //初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory  = new FileBasedChatMemory(fileDir);*/

        //初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();

        this.chatClient = ChatClient.builder(deshscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // new MessageChatMemoryAdvisor(chatMemory) 直接构造方法：不能配置一些参数，不灵活
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), //链式构造
                        new MyLoggerAdvisor()
                        //new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * Ai 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId)
                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY,10))//记忆是从下往上记忆的，后进先出
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        //log.info("content:{}",content);
        return content;
    }

    // 恋爱报告输出类（创建一个final类型的java类）
    record LoveReport(String title, List<String> suggestions){}
    /**
     * Ai 恋爱报告（结构化输出）
     */
    public LoveReport doChatWithReport(String message,String chatId){
        LoveReport LoveReportResponse = chatClient.prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY,10))//记忆是从下往上记忆的，后进先出
                .call()
                .entity(LoveReport.class);
        //log.info("content:{}",content);
        return LoveReportResponse;
    }

    /**
     * 和RAG知识库进行问答
     */
    public String doChatWithRag(String message,String chatId){
        //查询重写
        String reMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient.prompt()
                .user(reMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY,10))
                //应用RAG知识库问答(基于内存向量数据库)
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                //应用RAG检索增强服务（基于云知识库）
//                .advisors(loveAppRagCloudAdvisor)
                //应用RAG检索增强服务(基于PgVector向量数据库)
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                //应用自定义的RAG 检索增强服务(文档检索器 + 上下文查询增强器)
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore,"单身")
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }

    /**
     * AI 工具调用
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTool(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .tools(toolRegistration.allTools())
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }
}

