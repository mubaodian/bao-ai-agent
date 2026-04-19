import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoveChatView from '../views/LoveChatView.vue'
import ManusChatView from '../views/ManusChatView.vue'
import { updatePageSeo } from '../utils/seo'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: {
        title: 'BAO AI Agent | AI 应用导航与实时流式对话入口',
        description:
          '进入 BAO AI Agent 首页，快速切换 AI 恋爱大师与 AI 超级智能体应用，体验统一界面与流式对话能力。',
        keywords:
          'BAO AI Agent,AI 应用首页,AI 恋爱大师,AI 超级智能体,SSE 实时对话',
      },
    },
    {
      path: '/love',
      name: 'love',
      component: LoveChatView,
      meta: {
        title: 'AI 恋爱大师 | BAO AI Agent',
        description:
          'AI 恋爱大师提供连续单气泡流式回复，适合情感咨询、聊天润色与关系沟通场景。',
        keywords:
          'AI 恋爱大师,情感 AI,聊天润色,恋爱建议,SSE 流式回复',
      },
    },
    {
      path: '/manus',
      name: 'manus',
      component: ManusChatView,
      meta: {
        title: 'AI 超级智能体 | BAO AI Agent',
        description:
          'AI 超级智能体采用步骤式流式输出，每个 SSE 事件独立展示，适合复杂任务拆解与过程跟踪。',
        keywords:
          'AI 超级智能体,步骤式 AI,SSE 流式输出,任务拆解,智能体应用',
      },
    },
  ],
})

router.afterEach((to) => {
  updatePageSeo(to)
})

export default router
