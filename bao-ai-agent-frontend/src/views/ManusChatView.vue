<script setup>
import ChatShell from '../components/ChatShell.vue'
</script>

<template>
  <ChatShell
    title="AI 超级智能体"
    subtitle="把复杂问题交给流式智能体，一步步看到它的输出过程。"
    endpoint="/ai/manus/chat"
    prompt-key="userPrompt"
    theme="teal"
    assistant-bubble-mode="per-event"
    intro="欢迎来到 AI 超级智能体。这里会实时展示后端返回的流式内容。"
    placeholder="输入任务目标、问题背景或你希望智能体执行的内容..."
  />
</template>
