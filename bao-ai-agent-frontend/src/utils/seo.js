const DEFAULT_TITLE = 'BAO AI Agent | 实时流式 AI 对话平台'
const DEFAULT_DESCRIPTION =
  'BAO AI Agent 提供 AI 恋爱大师与 AI 超级智能体两种实时 SSE 对话体验，支持多端响应式访问。'
const DEFAULT_KEYWORDS =
  'BAO AI Agent,AI 恋爱大师,AI 超级智能体,SSE 流式对话,Vue3 AI 应用'

function ensureMeta(selector, attrName, attrValue) {
  let meta = document.head.querySelector(selector)

  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute(attrName, attrValue)
    document.head.appendChild(meta)
  }

  return meta
}

function setMetaName(name, content) {
  const meta = ensureMeta(`meta[name="${name}"]`, 'name', name)
  meta.setAttribute('content', content)
}

function setMetaProperty(property, content) {
  const meta = ensureMeta(`meta[property="${property}"]`, 'property', property)
  meta.setAttribute('content', content)
}

function setCanonical(url) {
  let link = document.head.querySelector('link[rel="canonical"]')

  if (!link) {
    link = document.createElement('link')
    link.setAttribute('rel', 'canonical')
    document.head.appendChild(link)
  }

  link.setAttribute('href', url)
}

function setStructuredData(payload) {
  let script = document.getElementById('structured-data')

  if (!script) {
    script = document.createElement('script')
    script.id = 'structured-data'
    script.type = 'application/ld+json'
    document.head.appendChild(script)
  }

  script.textContent = JSON.stringify(payload)
}

export function updatePageSeo(route) {
  const title = route.meta?.title ?? DEFAULT_TITLE
  const description = route.meta?.description ?? DEFAULT_DESCRIPTION
  const keywords = route.meta?.keywords ?? DEFAULT_KEYWORDS
  const path = route.fullPath || route.path || '/'
  const canonicalUrl = new URL(path, window.location.origin).toString()

  document.title = title

  setMetaName('description', description)
  setMetaName('keywords', keywords)
  setMetaName('twitter:title', title)
  setMetaName('twitter:description', description)
  setMetaProperty('og:title', title)
  setMetaProperty('og:description', description)
  setMetaProperty('og:url', canonicalUrl)
  setCanonical(canonicalUrl)

  setStructuredData({
    '@context': 'https://schema.org',
    '@type': 'WebApplication',
    name: title,
    description,
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Windows, macOS, Linux, Android, iOS',
    inLanguage: 'zh-CN',
    url: canonicalUrl,
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'CNY',
    },
  })
}

