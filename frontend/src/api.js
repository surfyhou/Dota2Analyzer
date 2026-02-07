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
