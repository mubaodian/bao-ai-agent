<script setup>
const props = defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
})

const year = new Date().getFullYear()

const navLinks = [
  { label: '首页', to: '/' },
  { label: 'AI 恋爱大师', to: '/love' },
  { label: 'AI 超级智能体', to: '/manus' },
]
</script>

<template>
  <footer class="site-footer" :class="{ compact }">
    <template v-if="compact">
      <div class="site-footer-compact">
        <nav class="site-footer-inline-links" aria-label="底部导航">
          <router-link
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
          >
            {{ link.label }}
          </router-link>
        </nav>
        <p>© {{ year }} BAO AI Agent 版权所有</p>
      </div>
    </template>

    <template v-else>
      <div class="site-footer-grid">
        <section>
          <h2>BAO AI Agent</h2>
          <p>实时流式 AI 对话平台，统一支持 AI 恋爱大师与 AI 超级智能体场景。</p>
        </section>

        <section>
          <h3>产品导航</h3>
          <nav class="site-footer-links" aria-label="产品导航">
            <router-link
              v-for="link in navLinks"
              :key="link.to"
              :to="link.to"
            >
              {{ link.label }}
            </router-link>
          </nav>
        </section>

        <section>
          <h3>联系与说明</h3>
          <p>支持基于 SSE 的实时对话输出</p>
          <p>适配 PC、平板与手机访问</p>
          <p>前端基于 Vue 3 + Vite 构建</p>
        </section>
      </div>

      <div class="site-footer-bottom">
        <p>© {{ year }} BAO AI Agent 版权所有</p>
        <span>仿照内容型产品网站底部信息区样式，展示产品信息与版权说明</span>
      </div>
    </template>
  </footer>
</template>
