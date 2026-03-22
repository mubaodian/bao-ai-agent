package com.swl.baoaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static{
        //取消手动注册，使用默认的InstantiatorStrategy
        kryo.setRegistrationRequired(false);
        //设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public FileBasedChatMemory(String dir){
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if(!baseDir.exists()){
            baseDir.mkdirs();
        }
    }

    /**
     * 添加一条会话消息
     * @param conversationId
     * @param message
     */
    @Override
    public void add(String conversationId, Message message) {
        saveConversation(conversationId,List.of(message));
    }

    /**
     * 添加多条会话消息
     * @param conversationId
     * @param messages
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        //把往期对话读取出来
        List<Message> messageList = getOrCreateConversation(conversationId);
        //合并新对话
        messageList.addAll(messages);
        //保存
        saveConversation(conversationId,messageList);
    }

    /**
     * 获取最近N条会话消息
     * @param conversationId
     * @param lastN
     * @return
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList.stream()
                .skip(Math.max(0,messageList.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if(file.exists()){
            file.delete();
        }
    }

    /**
     * 获取或创建会话消息的列表（读文件）
     * @param conversationId
     * @return
     */
    private List<Message> getOrCreateConversation(String conversationId){
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if(file.exists()){
            try(Input input = new Input(new FileInputStream(file))){
                messages = kryo.readObject(input,ArrayList.class);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 保存会话消息(写文件)
     */
    private void saveConversation(String conversationId,List<Message> messages){
        File file = getConversationFile(conversationId);
        try(Output output = new Output(new FileOutputStream(file))){
            kryo.writeObject(output,messages);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    /**
     * 每个会话对应一个文件，每个会话文件单独保存
     * @param conversationId
     * @return
     */
    private File getConversationFile(String conversationId){
       return new File(BASE_DIR,conversationId + ".kryo");
    }
}
