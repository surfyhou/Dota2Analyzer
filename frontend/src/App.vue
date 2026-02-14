<template>
  <div class="page">
    <div class="header">
      <div>
        <div class="header-title">Dota2 一号位复盘</div>
        <div class="header-subtitle">基于 OpenDota 数据生成选人轮次、对线期细节与关键错误建议</div>
      </div>
      <el-tag type="info" effect="dark">前后端分离 · Java + Vue</el-tag>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-button :type="view === 'analysis' ? 'primary' : 'default'" @click="view = 'analysis'">复盘</el-button>
        <el-button :type="view === 'preload' ? 'primary' : 'default'" @click="view = 'preload'">数据拉取</el-button>
      </div>
    </div>

    <div v-if="view === 'analysis'">
      <div class="panel">
        <div class="toolbar">
          <el-input v-model="accountId" placeholder="Steam32 账号ID" style="width: 220px" />
          <el-input-number v-model="limit" :min="1" :max="50" label="场次" />
          <el-switch v-model="onlyPos1" active-text="只看一号位" inactive-text="包含全部" />
          <el-button type="primary" :loading="loading" @click="runAnalysis">生成复盘</el-button>
        </div>
        <div v-if="error" style="margin-top: 12px; color: #fca5a5">{{ error }}</div>
      </div>

      <div v-if="summary" class="panel">
        <div class="summary">
          <div class="summary-card">
            <div class="summary-label">总场次</div>
            <div class="summary-value">{{ summary.totalMatches }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">胜率</div>
            <div class="summary-value">{{ summary.winRate.toFixed(1) }}%</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">胜场</div>
            <div class="summary-value">{{ summary.wins }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">未解析</div>
            <div class="summary-value">{{ summary.unparsedMatches }}</div>
          </div>
        </div>
      </div>

      <div class="panel">
        <el-table :data="matches" v-loading="loading" stripe style="width: 100%">
          <el-table-column type="expand">
            <template #default="props">
              <div style="padding: 12px 24px">
                <p><strong>对线</strong>：{{ props.row.laneResult }}</p>
                <p><strong>对线组合</strong>：
                  <span class="hero-row-sm">
                    <img class="hero-icon-sm" :src="getHeroImg(props.row.heroId)" :title="props.row.heroName" />
                    <img v-for="id in props.row.laneAllyHeroIds" :key="'la'+id" class="hero-icon-sm" :src="getHeroImg(id)" :title="getHeroNameById(props.row, id, true)" />
                    <span class="vs-label">vs</span>
                    <img v-for="id in props.row.laneEnemyHeroIds" :key="'le'+id" class="hero-icon-sm" :src="getHeroImg(id)" :title="getHeroNameById(props.row, id, false)" />
                  </span>
                </p>
                <p v-if="props.row.laneKills || props.row.laneDeaths"><strong>对线击杀</strong>：己方 {{ props.row.laneKills }} 次击杀，被杀 {{ props.row.laneDeaths }} 次</p>
                <p><strong>选人轮次</strong>：{{ props.row.pickRound }}（第 {{ props.row.pickIndex }} 手）</p>
                <p><strong>队友阵容</strong>：
                  <span class="hero-row-sm">
                    <img v-for="(id, i) in props.row.allyHeroIds" :key="'a'+id" class="hero-icon-sm" :src="getHeroImg(id)" :title="props.row.allyHeroes[i]" />
                  </span>
                  <span v-if="!props.row.allyHeroIds?.length" style="color: var(--text-secondary)">未知</span>
                </p>
                <p><strong>敌方阵容</strong>：
                  <span class="hero-row-sm">
                    <img v-for="(id, i) in props.row.enemyHeroIds" :key="'e'+id" class="hero-icon-sm" :src="getHeroImg(id)" :title="props.row.enemyHeroes[i]" />
                  </span>
                  <span v-if="!props.row.enemyHeroIds?.length" style="color: var(--text-secondary)">未知</span>
                </p>

                <div style="margin: 12px 0 6px">
                  <strong>装备时间轴</strong>
                </div>
                <el-slider
                  v-if="props.row.inventoryTimeline?.length"
                  v-model="inventorySlider[props.row.matchId]"
                  :min="0"
                  :max="props.row.inventoryTimeline.at(-1).time"
                  :step="60"
                  show-stops
                />
                <div v-if="props.row.inventoryTimeline?.length" style="margin-bottom: 8px; color: rgba(248,250,252,0.8)">
                  {{ formatTime(inventorySlider[props.row.matchId]) }} 时刻装备：
                </div>
                <div v-if="props.row.inventoryTimeline?.length" class="inventory-grid">
                  <div v-for="item in getItemsAtTime(props.row, inventorySlider[props.row.matchId])" :key="item.key" class="inventory-item">
                    <img v-if="getItemImg(item)" :src="getItemImg(item)" :alt="item.name" />
                    <div v-else class="inventory-fallback">{{ item.name }}</div>
                    <div class="inventory-name">{{ item.name }}</div>
                  </div>
                  <div v-if="getItemsAtTime(props.row, inventorySlider[props.row.matchId]).length === 0" style="color: rgba(248,250,252,0.6)">
                    暂无
                  </div>
                </div>

                <p><strong>对线细节</strong>：</p>
                <ul class="mistake-list">
                  <li v-for="(item, index) in props.row.laningDetails || []" :key="index">{{ item }}</li>
                  <li v-if="!props.row.laningDetails || props.row.laningDetails.length === 0">无</li>
                </ul>
                <p style="margin-top: 10px"><strong>基准对比（英雄分位）</strong>：</p>
                <ul class="mistake-list">
                  <li v-for="(item, index) in props.row.benchmarkNotes || []" :key="index">{{ item }}</li>
                  <li v-if="!props.row.benchmarkNotes || props.row.benchmarkNotes.length === 0">无</li>
                </ul>
                <p><strong>关键错误</strong>：</p>
                <ul class="mistake-list">
                  <li v-for="(item, index) in props.row.mistakes" :key="index">{{ item }}</li>
                </ul>
                <p style="margin-top: 10px"><strong>建议</strong>：</p>
                <ul class="mistake-list">
                  <li v-for="(item, index) in props.row.suggestions" :key="index">{{ item }}</li>
                </ul>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="matchId" label="对局ID" width="140" />
          <el-table-column label="英雄" width="170">
            <template #default="props">
              <div class="hero-cell">
                <img class="hero-icon-inline" :src="getHeroImg(props.row.heroId)" :alt="props.row.heroName" />
                <span>{{ props.row.heroName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="props">
              <el-tag :class="props.row.won ? 'tag-win' : 'tag-loss'" effect="dark">
                {{ props.row.resultText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="performanceRating" label="表现" width="150" />
          <el-table-column prop="laneResult" label="对线" min-width="220" />
          <el-table-column label="KDA" width="120">
            <template #default="props">
              {{ props.row.statistics?.KDA || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="GPM/XPM" width="130">
            <template #default="props">
              {{ props.row.statistics?.['GPM/XPM'] || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-else class="panel">
      <div class="toolbar">
        <el-input v-model="accountId" placeholder="Steam32 账号ID" style="width: 220px" />
        <el-input-number v-model="preloadCount" :min="20" :max="200" label="拉取场次" />
        <el-button type="primary" :loading="preloadLoading" @click="startPreload">开始拉取</el-button>
        <el-button @click="refreshStatus">刷新状态</el-button>
        <el-button @click="loadCachedMatches">加载缓存</el-button>
      </div>
      <div v-if="preloadStatus" style="margin-top: 16px">
        <div>状态：{{ preloadStatus.message }}</div>
        <div>完成：{{ preloadStatus.completed }}/{{ preloadStatus.total }}，失败：{{ preloadStatus.failed }}</div>
        <div>更新时间：{{ new Date(preloadStatus.lastUpdated).toLocaleString() }}</div>
        <el-progress :percentage="progressPercent" style="margin-top: 10px" />
      </div>
      <div v-if="cacheSummary" style="margin-top: 12px; color: rgba(248,250,252,0.75)">
        {{ cacheSummary }}
      </div>
      <div v-if="cacheMessage" style="margin-top: 12px; color: rgba(248,250,252,0.7)">
        {{ cacheMessage }}
      </div>

      <div v-if="displayMatches.length" class="panel" style="margin-top: 16px">
        <div class="summary-label" style="margin-bottom: 8px">拉取详情</div>
        <div class="preload-list">
          <div v-for="row in displayMatches" :key="row.matchId" class="preload-row">
            <div class="preload-side">
              <div class="side-label">天辉</div>
              <div class="hero-row">
                <img v-for="heroId in row.radiantHeroes" :key="heroId" :src="getHeroImg(heroId)" :alt="heroId" />
              </div>
            </div>
            <div class="preload-center">
              <div class="result" :class="row.radiantWin ? 'win-radiant' : 'win-dire'">
                {{ row.radiantWin ? '天辉胜利' : '夜魇胜利' }}
              </div>
              <div class="meta">
                时长 {{ Math.floor(row.duration / 60) }}:{{ (row.duration % 60).toString().padStart(2, '0') }}
              </div>
              <div class="meta">Match {{ row.matchId }}</div>
            </div>
            <div class="preload-side">
              <div class="side-label">夜魇</div>
              <div class="hero-row">
                <img v-for="heroId in row.direHeroes" :key="heroId" :src="getHeroImg(heroId)" :alt="heroId" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { analyzeRecentMatches, preloadMatches, fetchPreloadStatus, getHeroImageUrl, getItemImageUrl, fetchCachedMatches } from './api'

const accountId = ref('')
const limit = ref(20)
const onlyPos1 = ref(true)
const loading = ref(false)
const error = ref('')
const summary = ref(null)
const matches = ref([])
const inventorySlider = ref({})
const view = ref('analysis')

const preloadCount = ref(100)
const preloadLoading = ref(false)
const preloadStatus = ref(null)
const cachedMatches = ref([])
const cacheMessage = ref('')
const pollTimer = ref(null)

const runAnalysis = async () => {
  if (!accountId.value) {
    error.value = '请输入 Steam32 账号ID'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const data = await analyzeRecentMatches(accountId.value, limit.value, onlyPos1.value)
    summary.value = data.summary
    matches.value = data.matches
    inventorySlider.value = data.matches.reduce((acc, match) => {
      const lastTime = match.inventoryTimeline?.length ? match.inventoryTimeline.at(-1).time : 0
      acc[match.matchId] = lastTime
      return acc
    }, {})
  } catch (err) {
    error.value = err?.response?.data?.message || '请求失败，请检查后端服务或账号ID'
  } finally {
    loading.value = false
  }
}

const startPreload = async () => {
  if (!accountId.value) {
    error.value = '请输入 Steam32 账号ID'
    return
  }
  preloadLoading.value = true
  try {
    preloadStatus.value = await preloadMatches(accountId.value, preloadCount.value)
    startPolling()
  } catch (err) {
    error.value = err?.response?.data?.message || '拉取失败，请检查后端服务'
  } finally {
    preloadLoading.value = false
  }
}

const refreshStatus = async () => {
  if (!accountId.value) return
  preloadStatus.value = await fetchPreloadStatus(accountId.value)
  if (preloadStatus.value && !preloadStatus.value.isRunning) {
    stopPolling()
    await loadCachedMatches()
  }
}

const loadCachedMatches = async () => {
  if (!accountId.value) return
  cacheMessage.value = ''
  try {
    const data = await fetchCachedMatches(accountId.value, preloadCount.value)
    cachedMatches.value = data
    if (data.length === 0) {
      cacheMessage.value = '缓存为空或没有找到对应账号的对局，请先点击“开始拉取”。'
    }
  } catch (err) {
    cacheMessage.value = err?.response?.data?.message || '加载缓存失败，请检查后端服务'
  }
}

const displayMatches = computed(() => {
  if (preloadStatus.value?.matches?.length) return preloadStatus.value.matches
  return cachedMatches.value
})

const cacheSummary = computed(() => {
  if (!accountId.value) return ''
  if (cachedMatches.value.length === 0) return cacheMessage.value
  return `缓存中已有 ${cachedMatches.value.length} 场比赛`
})

const progressPercent = computed(() => {
  if (!preloadStatus.value || !preloadStatus.value.total) return 0
  return Math.min(100, Math.round((preloadStatus.value.completed / preloadStatus.value.total) * 100))
})

const formatTime = (seconds = 0) => {
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}:${secs.toString().padStart(2, '0')}`
}

const getItemsAtTime = (match, seconds = 0) => {
  const timeline = match.inventoryTimeline || []
  if (timeline.length === 0) return []
  let snapshot = timeline[0]
  for (const point of timeline) {
    if (point.time <= seconds) snapshot = point
  }
  return snapshot.items || []
}

const getHeroImg = (heroId) => getHeroImageUrl(heroId)
const getHeroNameById = (row, heroId, isAlly) => {
  const ids = isAlly ? (row.laneAllyHeroIds || []) : (row.laneEnemyHeroIds || [])
  const names = isAlly ? (row.laneAllyHeroes || []) : (row.laneEnemyHeroes || [])
  const idx = ids.indexOf(heroId)
  return idx >= 0 ? names[idx] : ''
}
const getItemImg = (item) => {
  if (!item?.key) return ''
  return getItemImageUrl(item.key)
}

const startPolling = () => {
  if (pollTimer.value) return
  pollTimer.value = setInterval(() => {
    refreshStatus()
  }, 2000)
}

const stopPolling = () => {
  if (!pollTimer.value) return
  clearInterval(pollTimer.value)
  pollTimer.value = null
}

watch(view, async (val) => {
  if (val === 'preload') {
    await loadCachedMatches()
    await refreshStatus()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>
