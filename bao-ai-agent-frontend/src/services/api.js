import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

export function buildApiUrl(url, params = {}) {
  return api.getUri({ url, params })
}

