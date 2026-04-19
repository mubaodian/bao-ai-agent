<script setup>
import SiteFooter from '../components/SiteFooter.vue'

const apps = [
  {
    path: '/love',
    name: 'AI 恋爱大师',
    tag: 'LOVE APP',
    description: '带独立 chatId 的流式聊天页，适合多轮恋爱话术和关系建议场景。',
    tone: '多轮情感对话',
    feature: '单条回复持续拼接，适合完整陪伴感表达。',
  },
  {
    path: '/manus',
    name: 'AI 超级智能体',
    tag: 'MANUS AGENT',
    description: '同样采用 SSE 实时输出，适合复杂任务拆解和连续思考场景。',
    tone: '步骤式智能体输出',
    feature: '每个 SSE 步骤独立成气泡，便于追踪执行路径。',
  },
]

const highlights = [
  '统一玻璃拟态界面',
  'PC / 平板 / 手机响应式兼容',
  '实时 SSE 对话与打字机反馈',
]

const metrics = [
  { value: '02', label: '核心 AI 应用' },
  { value: 'SSE', label: '实时流式链路' },
  { value: '24/7', label: '持续交互体验' },
]

const commandLines = [
  '$ boot bao-ai-agent --mode realtime',
  '> load apps: love_app, manus_agent',
  '> status: streaming ready',
]
</script>

<template>
  <main class="home-page">
    <div class="home-backdrop"></div>
    <div class="home-gridlines"></div>
    <section class="home-shell">
      <section class="hero hero-geek">
        <div class="hero-copy-block hero-main-panel">
          <div class="hero-chip-row">
            <span class="hero-chip">SYSTEM ONLINE</span>
            <span class="hero-chip muted">STREAM READY</span>
          </div>
          <p class="eyebrow">BAO AI AGENT</p>
          <h1>不是普通导航页，是一块可以直接点燃对话的 AI 控制台。</h1>
          <p class="hero-copy">
            两个 AI 应用，一套高压感视觉系统。进入页面后，不是“看看功能”，而是直接接通流式输出、步骤反馈和持续交互。
          </p>

          <div class="hero-metrics">
            <article
              v-for="metric in metrics"
              :key="metric.label"
              class="hero-metric"
            >
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
            </article>
          </div>
        </div>

        <aside class="overview-card terminal-card">
          <div class="terminal-head">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span class="overview-label">SYSTEM FEED</span>
          <strong>实时、稳定、连续输出。</strong>
          <div class="terminal-lines">
            <p v-for="line in commandLines" :key="line">{{ line }}</p>
          </div>
          <ul class="overview-list">
            <li v-for="item in highlights" :key="item">{{ item }}</li>
          </ul>
        </aside>
      </section>

      <section class="app-grid geek-grid">
        <router-link
          v-for="app in apps"
          :key="app.path"
          :to="app.path"
          class="app-card geek-card"
        >
          <div class="app-card-top">
            <span class="app-tag">{{ app.tag }}</span>
            <span class="app-tone">{{ app.tone }}</span>
          </div>
          <div class="card-orbit"></div>
          <h2>{{ app.name }}</h2>
          <p>{{ app.description }}</p>
          <small>{{ app.feature }}</small>
          <strong>进入应用</strong>
        </router-link>
      </section>
    </section>

    <SiteFooter />
  </main>
</template>
