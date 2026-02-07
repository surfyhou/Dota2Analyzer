<template>
  <div class="page">
    <div class="header">
      <div>
        <div class="header-title">Dota2 一号位复盘</div>
        <div class="header-subtitle">基于 OpenDota 数据生成选人轮次、对线期细节与关键错误建议</div>
      </div>
      <el-tag type="info" effect="dark">前后端分离 · C# + Vue</el-tag>
    </div>

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
              <p><strong>对位英雄</strong>：{{ props.row.laneOpponentHero || '未知' }}</p>
              <p><strong>选人轮次</strong>：{{ props.row.pickRound }}（第 {{ props.row.pickIndex }} 手）</p>
              <p><strong>队友阵容</strong>：{{ props.row.allyHeroes.join('、') || '未知' }}</p>
              <p><strong>敌方阵容</strong>：{{ props.row.enemyHeroes.join('、') || '未知' }}</p>
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
        <el-table-column prop="heroName" label="英雄" width="140" />
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
</template>

<script setup>
import { ref } from 'vue'
import { analyzeRecentMatches } from './api'

const accountId = ref('')
const limit = ref(20)
const onlyPos1 = ref(true)
const loading = ref(false)
const error = ref('')
const summary = ref(null)
const matches = ref([])

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
  } catch (err) {
    error.value = err?.response?.data?.message || '请求失败，请检查后端服务或账号ID'
  } finally {
    loading.value = false
  }
}
</script>
