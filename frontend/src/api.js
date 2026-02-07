import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:5086/api',
  timeout: 20000
})

export async function fetchRecentMatches(accountId, limit) {
  const { data } = await api.get(`/players/${accountId}/recent`, {
    params: { limit }
  })
  return data
}

export async function analyzeRecentMatches(accountId, limit, onlyPos1 = true) {
  const { data } = await api.post(`/players/${accountId}/analyze-recent`, null, {
    params: { limit, requestParse: true, onlyPos1 }
  })
  return data
}

export async function analyzeMatch(matchId, accountId) {
  const { data } = await api.get(`/matches/${matchId}/analyze`, {
    params: { accountId, requestParse: true }
  })
  return data
}

export async function preloadMatches(accountId, count = 100) {
  const { data } = await api.post(`/players/${accountId}/preload`, null, {
    params: { count }
  })
  return data
}

export async function fetchPreloadStatus(accountId) {
  const { data } = await api.get(`/players/${accountId}/preload-status`)
  return data
}

export function getHeroImageUrl(heroId) {
  return `${api.defaults.baseURL.replace('/api', '')}/api/assets/heroes/${heroId}`
}

export function getItemImageUrl(itemKey) {
  const normalized = String(itemKey || '').replace(/^item_/i, '')
  return `${api.defaults.baseURL.replace('/api', '')}/api/assets/items/${encodeURIComponent(normalized)}`
}

export async function fetchCachedMatches(accountId, count = 100) {
  const { data } = await api.get(`/players/${accountId}/cached-matches`, {
    params: { count }
  })
  return data
}
