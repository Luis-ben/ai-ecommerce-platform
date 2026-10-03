<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify } from '../store'

type AiMonitorData = {
  total_conversations: number; today_conversations: number
  ai_response_rate: number; avg_response_time_ms: number
  tickets_created: number; tickets_resolved: number; resolution_rate: number
  intent_distribution: { label: string; count: number; pct: number }[]
  hourly_messages: { hour: number; count: number }[]
  recent_conversations: { id: string; user: string; intent: string; messages: number; created_at: string; status: string }[]
  top_queries: { query: string; count: number }[]
}

const data = ref<AiMonitorData | null>(null)
const loading = ref(true)

// 生成演示数据（当后端未配置时使用）
function mockData(): AiMonitorData {
  const hours = Array.from({ length: 24 }, (_, i) => ({
    hour: i, count: Math.round(Math.random() * 15 + (i >= 9 && i <= 21 ? 10 : 2))
  }))
  return {
    total_conversations: 1284, today_conversations: 47,
    ai_response_rate: 94.3, avg_response_time_ms: 820,
    tickets_created: 38, tickets_resolved: 31, resolution_rate: 81.6,
    intent_distribution: [
      { label: '商品咨询', count: 512, pct: 39.9 },
      { label: '订单查询', count: 334, pct: 26.0 },
      { label: '退货申请', count: 198, pct: 15.4 },
      { label: '价格询问', count: 143, pct: 11.1 },
      { label: '其他投诉', count: 97, pct: 7.6 },
    ],
    hourly_messages: hours,
    recent_conversations: [
      { id: 'conv-001', user: 'customer@example.com', intent: '商品咨询', messages: 6, created_at: new Date().toISOString(), status: 'CLOSED' },
      { id: 'conv-002', user: 'kele@qq.com', intent: '退货申请', messages: 4, created_at: new Date(Date.now() - 3600000).toISOString(), status: 'OPEN' },
      { id: 'conv-003', user: '123@guangda.work', intent: '订单查询', messages: 3, created_at: new Date(Date.now() - 7200000).toISOString(), status: 'CLOSED' },
      { id: 'conv-004', user: 'test@qq.com', intent: '价格询问', messages: 5, created_at: new Date(Date.now() - 10800000).toISOString(), status: 'CLOSED' },
    ],
    top_queries: [
      { query: '这件商品还有货吗？', count: 87 },
      { query: '我的订单什么时候发货？', count: 65 },
      { query: '可以退货吗？', count: 54 },
      { query: '这个价格能优惠吗？', count: 42 },
      { query: '支持哪些支付方式？', count: 38 },
    ],
  }
}

// SVG 折线图
const SVG_W = 600; const SVG_H = 120
const hourMax = computed(() => Math.max(1, ...(data.value?.hourly_messages.map(h => h.count) || [1])))
function hLinePoints(items: { hour: number; count: number }[]): string {
  if (!items.length) return ''
  return items.map((h) => `${(h.hour / 23) * (SVG_W - 20) + 10},${SVG_H - 10 - (h.count / hourMax.value) * (SVG_H - 20)}`).join(' ')
}

// 意图颜色
const intentColors = ['#fd7e14', '#0d6efd', '#dc3545', '#20c997', '#6f42c1']

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await api<AiMonitorData>('/api/admin/ai/monitor').catch(() => null)
    data.value = result || mockData()
  } catch { data.value = mockData() }
  finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <AdminShell active="ai-monitor" title="AI 监控">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请以 merchant@example.com 登录。</p>
    <template v-else>
      <!-- 顶部操作 -->
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 class="fw-bold mb-1">AI 监控</h5>
          <small class="text-secondary">实时监控 AI 客服对话质量和响应效率</small>
        </div>
        <button class="btn btn-sm btn-outline-secondary" @click="load">
          <i class="bi bi-arrow-clockwise me-1"></i>刷新
        </button>
      </div>

      <div v-if="loading" class="text-center py-5 text-secondary">
        <i class="bi bi-hourglass-split fs-2 d-block mb-2"></i>加载中…
      </div>
      <template v-else-if="data">
        <!-- KPI 卡片 -->
        <div class="row g-3 mb-4">
          <div class="col-6 col-xl-3">
            <div class="card text-center py-3">
              <div class="text-secondary small mb-1">累计对话数</div>
              <div class="fs-2 fw-bold text-primary">{{ data.total_conversations.toLocaleString() }}</div>
              <div class="text-secondary" style="font-size:12px">今日 +{{ data.today_conversations }}</div>
            </div>
          </div>
          <div class="col-6 col-xl-3">
            <div class="card text-center py-3">
              <div class="text-secondary small mb-1">AI 应答率</div>
              <div class="fs-2 fw-bold text-success">{{ data.ai_response_rate }}%</div>
              <div class="text-secondary" style="font-size:12px">未触达转人工</div>
            </div>
          </div>
          <div class="col-6 col-xl-3">
            <div class="card text-center py-3">
              <div class="text-secondary small mb-1">平均响应时间</div>
              <div class="fs-2 fw-bold text-warning">{{ (data.avg_response_time_ms / 1000).toFixed(1) }}s</div>
              <div class="text-secondary" style="font-size:12px">含网络延迟</div>
            </div>
          </div>
          <div class="col-6 col-xl-3">
            <div class="card text-center py-3">
              <div class="text-secondary small mb-1">工单解决率</div>
              <div class="fs-2 fw-bold text-info">{{ data.resolution_rate }}%</div>
              <div class="text-secondary" style="font-size:12px">{{ data.tickets_resolved }}/{{ data.tickets_created }} 已解决</div>
            </div>
          </div>
        </div>

        <!-- 中间：小时消息趋势 + 意图分布 -->
        <div class="row g-3 mb-4">
          <!-- 消息趋势折线 -->
          <div class="col-12 col-xl-7">
            <div class="card h-100">
              <div class="card-body">
                <h6 class="card-title fw-bold mb-3">消息量趋势（24小时）</h6>
                <svg :viewBox="`0 0 ${SVG_W} ${SVG_H}`" class="w-100" style="height:130px">
                  <defs>
                    <linearGradient id="msgGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#0d6efd" stop-opacity="0.3"/>
                      <stop offset="100%" stop-color="#0d6efd" stop-opacity="0"/>
                    </linearGradient>
                  </defs>
                  <!-- 网格 -->
                  <line v-for="y in [0.25, 0.5, 0.75, 1.0]" :key="y"
                    x1="10" :y1="SVG_H - 10 - y * (SVG_H - 20)"
                    :x2="SVG_W - 10" :y2="SVG_H - 10 - y * (SVG_H - 20)"
                    stroke="#f0f0f0" stroke-width="1" />
                  <!-- 面积 -->
                  <polygon
                    :points="'10,' + (SVG_H - 10) + ' ' + hLinePoints(data.hourly_messages) + ' ' + (SVG_W - 10) + ',' + (SVG_H - 10)"
                    fill="url(#msgGrad)" />
                  <polyline :points="hLinePoints(data.hourly_messages)" fill="none" stroke="#0d6efd" stroke-width="2" stroke-linejoin="round" />
                  <!-- 点 -->
                  <circle v-for="h in data.hourly_messages" :key="h.hour"
                    :cx="(h.hour / 23) * (SVG_W - 20) + 10"
                    :cy="SVG_H - 10 - (h.count / hourMax) * (SVG_H - 20)"
                    r="2.5" fill="#0d6efd" />
                  <!-- X轴 -->
                  <text v-for="h in [0, 6, 12, 18, 23]" :key="'x' + h"
                    :x="(h / 23) * (SVG_W - 20) + 10" :y="SVG_H + 2"
                    text-anchor="middle" font-size="9" fill="#999">{{ String(h).padStart(2, '0') }}:00</text>
                </svg>
              </div>
            </div>
          </div>

          <!-- 意图分布 -->
          <div class="col-12 col-xl-5">
            <div class="card h-100">
              <div class="card-body">
                <h6 class="card-title fw-bold mb-3">意图分布</h6>
                <div v-for="(intent, i) in data.intent_distribution" :key="intent.label" class="mb-3">
                  <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="d-flex align-items-center gap-2 small">
                      <span class="rounded-circle d-inline-block" :style="{ width: '10px', height: '10px', background: intentColors[i % intentColors.length] }"></span>
                      {{ intent.label }}
                    </span>
                    <span class="small fw-bold">{{ intent.count }} <span class="text-secondary fw-normal">({{ intent.pct }}%)</span></span>
                  </div>
                  <div class="progress" style="height:6px">
                    <div class="progress-bar" :style="{ width: intent.pct + '%', background: intentColors[i % intentColors.length] }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 下方：高频问题 + 最近对话 -->
        <div class="row g-3">
          <!-- 高频问题 -->
          <div class="col-12 col-xl-4">
            <div class="card h-100">
              <div class="card-header"><h6 class="card-title fw-bold mb-0">高频提问 TOP5</h6></div>
              <div class="card-body p-0">
                <div v-for="(q, i) in data.top_queries" :key="q.query" class="d-flex align-items-center gap-3 px-3 py-2 border-bottom">
                  <span class="fw-bold" :style="{ color: i < 3 ? '#fd7e14' : '#aaa', minWidth: '20px' }">{{ i + 1 }}</span>
                  <span class="flex-grow-1 small text-truncate" :title="q.query">{{ q.query }}</span>
                  <span class="badge bg-light text-dark">{{ q.count }}次</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 最近对话 -->
          <div class="col-12 col-xl-8">
            <div class="card h-100">
              <div class="card-header d-flex justify-content-between align-items-center">
                <h6 class="card-title fw-bold mb-0">最近对话</h6>
                <a class="text-primary small" style="cursor:pointer">查看全部</a>
              </div>
              <div class="card-body p-0">
                <table class="table table-hover mb-0 align-middle">
                  <thead><tr><th class="ps-3">用户</th><th>意图</th><th>消息数</th><th>状态</th><th>时间</th></tr></thead>
                  <tbody>
                    <tr v-for="conv in data.recent_conversations" :key="conv.id">
                      <td class="ps-3 small">{{ conv.user }}</td>
                      <td><span class="badge bg-light text-dark small">{{ conv.intent }}</span></td>
                      <td class="text-secondary small">{{ conv.messages }}</td>
                      <td>
                        <span class="badge" :class="conv.status === 'CLOSED' ? 'bg-secondary' : 'bg-success'">
                          {{ conv.status === 'CLOSED' ? '已关闭' : '进行中' }}
                        </span>
                      </td>
                      <td class="text-secondary small text-nowrap">{{ conv.created_at.replace('T', ' ').slice(11, 19) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </AdminShell>
</template>
