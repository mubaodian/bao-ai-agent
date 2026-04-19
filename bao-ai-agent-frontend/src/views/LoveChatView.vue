<script setup>
import ChatShell from '../components/ChatShell.vue'
</script>

<template>
  <ChatShell
    title="AI 恋爱大师"
    subtitle="让每一句回复都更懂分寸、情绪和关系节奏。"
    endpoint="/ai/love_app/chat/sse"
    prompt-key="message"
    theme="rose"
    :use-chat-id="true"
    intro="欢迎来到 AI 恋爱大师。当前页面会自动生成独立聊天室 ID，方便区分不同会话。"
    placeholder="输入你想咨询的聊天内容、关系进展或回复草稿..."
  />
</template>

