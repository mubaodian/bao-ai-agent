import axios from 'axios'

function normalizeBaseUrl(value) {
  const raw = `${value ?? ''}`.trim()

  if (!raw) {
    return '/api'
  }

  if (/^https?:\/\//i.test(raw)) {
    return raw.replace(/\/+$/, '')
  }

  const normalizedPath = raw.replace(/^\/+/, '').replace(/\/+$/, '')
  return normalizedPath ? `/${normalizedPath}` : ''
}

export const API_BASE_URL = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

export function buildApiUrl(url, params = {}) {
  return api.getUri({ url, params })
}
