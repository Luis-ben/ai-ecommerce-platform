<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { navigate } from '../router'
import { isStaff, notify } from '../store'

type DashStats = {
  visitors: number; visitors_diff: number
  cart_adds: number; cart_adds_diff: number
  paid_users: number; paid_users_diff: number
  conversion_rate: number; conversion_rate_diff: number
  pv_by_hour: number[]; uv_by_hour: number[]
  funnel: { label: string; value: number }[]
  hot_products: { product_id: string; title: string; price: number; quantity: number; cover: string }[]
  slow_products: { product_id: string; title: string; price: number; quantity: number; cover: string }[]
  source_analysis: { label: string; value: number; pct: number }[]
  recent_orders_amount: number; recent_orders_count: number
  recent_tickets_open: number; recent_tickets_total: number
}

const stats = ref<DashStats | null>(null)
const loading = ref(true)
const periodTab = ref<'today' | 'yesterday' | 'week'>('today')

onMounted(async () => {
  try {
    stats.value = await api<DashStats>('/api/admin/dashboard/stats')
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
})

// ── SVG chart helpers ──
const SVG_W = 280; const SVG_H = 80
function sparkPoints(data: number[], color: string): string {
  if (!data.length) return ''
  const max = Math.max(...data, 1)
  const pts = data.map((v, i) => `${(i / (data.length - 1)) * SVG_W},${SVG_H - (v / max) * (SVG_H - 10) - 2}`).join(' ')
  return pts
}

const pvHours = computed(() => Array.from({ length: 24 }, (_, i) => i))
function lineY(v: number, max: number): number { return 130 - (v / (max || 1)) * 110 }
const pvMax = computed(() => Math.max(...(stats.value?.pv_by_hour || [1]), 1))
const pvPoints = computed(() => {
  const d = stats.value?.pv_by_hour ?? []
  return d.map((v, i) => `${(i / 23) * 540},${lineY(v, pvMax.value)}`).join(' ')
})
const uvPoints = computed(() => {
  const d = stats.value?.uv_by_hour ?? []
  return d.map((v, i) => `${(i / 23) * 540},${lineY(v, pvMax.value)}`).join(' ')
})
const funnelMax = computed(() => Math.max(...(stats.value?.funnel.map(f => f.value) ?? [1]), 1))
const sourceMax = computed(() => Math.max(...(stats.value?.source_analysis.map(s => s.value) ?? [1]), 1))
</script>

<template>
  <AdminShell active="home" title="数据看板">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要客服或商户角色，请以 merchant@example.com 登录。</p>
    <template v-else>
      <!-- 周期切换 -->
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 class="mb-1 fw-bold">数据看板</h5>
          <small class="text-secondary">实时监控电商平台关键指标和业务表现</small>
        </div>
        <div class="d-flex gap-2 align-items-center">
          <div class="btn-group">
            <button class="btn btn-sm" :class="periodTab === 'today' ? 'btn-warning' : 'btn-outline-secondary'" @click="periodTab = 'today'">今日</button>
            <button class="btn btn-sm" :class="periodTab === 'yesterday' ? 'btn-warning' : 'btn-outline-secondary'" @click="periodTab = 'yesterday'">昨日</button>
            <button class="btn btn-sm" :class="periodTab === 'week' ? 'btn-warning' : 'btn-outline-secondary'" @click="periodTab = 'week'">近7日</button>
          </div>
          <button class="btn btn-sm btn-outline-secondary" @click="navigate('/admin/reports')"><i class="bi bi-download me-1"></i>导出报表</button>
          <button class="btn btn-sm btn-outline-secondary" @click="loading = true; api('/api/admin/dashboard/stats').then(d => { stats = d as any }).catch(()=>{}).finally(() => loading = false)"><i class="bi bi-arrow-clockwise me-1"></i>刷新数据</button>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="text-center py-5 text-secondary"><i class="bi bi-hourglass-split fs-2 me-2"></i>加载中…</div>
      <template v-else-if="stats">
        <!-- KPI 卡片 -->
        <div class="row g-3 mb-4">
          <!-- 访客数 -->
          <div class="col-6 col-xl-3">
            <div class="card dash-kpi-card">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-1">
                  <span class="text-secondary small">访客数</span>
                  <span class="kpi-icon kpi-orange"><i class="bi bi-people-fill"></i></span>
                </div>
                <div class="kpi-value">{{ stats.visitors }}</div>
                <div class="kpi-diff" :class="stats.visitors_diff >= 0 ? 'text-danger' : 'text-success'">
                  <i :class="stats.visitors_diff >= 0 ? 'bi bi-arrow-up-short' : 'bi bi-arrow-down-short'"></i>
                  {{ Math.abs(stats.visitors_diff) }}% vs 昨日
                </div>
                <svg :width="SVG_W" height="50" class="mt-2 kpi-sparkline">
                  <polyline :points="sparkPoints(stats.pv_by_hour, '#fd7e14')" fill="none" stroke="#fd7e14" stroke-width="1.5" />
                </svg>
              </div>
            </div>
          </div>
          <!-- 加购数 -->
          <div class="col-6 col-xl-3">
            <div class="card dash-kpi-card">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-1">
                  <span class="text-secondary small">加购数</span>
                  <span class="kpi-icon kpi-blue"><i class="bi bi-cart-plus-fill"></i></span>
                </div>
                <div class="kpi-value">{{ stats.cart_adds }}</div>
                <div class="kpi-diff" :class="stats.cart_adds_diff >= 0 ? 'text-success' : 'text-danger'">
                  <i :class="stats.cart_adds_diff >= 0 ? 'bi bi-arrow-up-short' : 'bi bi-arrow-down-short'"></i>
                  {{ Math.abs(stats.cart_adds_diff) }}% vs 昨日
                </div>
                <svg :width="SVG_W" height="50" class="mt-2 kpi-sparkline">
                  <polyline :points="sparkPoints(stats.uv_by_hour, '#0d6efd')" fill="none" stroke="#0d6efd" stroke-width="1.5" />
                </svg>
              </div>
            </div>
          </div>
          <!-- 成交用户数 -->
          <div class="col-6 col-xl-3">
            <div class="card dash-kpi-card">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-1">
                  <span class="text-secondary small">成交用户数</span>
                  <span class="kpi-icon kpi-green"><i class="bi bi-person-check-fill"></i></span>
                </div>
                <div class="kpi-value">{{ stats.paid_users }}</div>
                <div class="kpi-diff" :class="stats.paid_users_diff >= 0 ? 'text-success' : 'text-danger'">
                  <i :class="stats.paid_users_diff >= 0 ? 'bi bi-arrow-up-short' : 'bi bi-arrow-down-short'"></i>
                  {{ Math.abs(stats.paid_users_diff) }}% vs 昨日
                </div>
                <div class="mt-2 small text-secondary">订单金额 ¥{{ stats.recent_orders_amount.toFixed(2) }}</div>
              </div>
            </div>
          </div>
          <!-- 转化率 -->
          <div class="col-6 col-xl-3">
            <div class="card dash-kpi-card">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-1">
                  <span class="text-secondary small">转化率 <span class="text-primary" style="font-size:11px">访客到购买转化率</span></span>
                  <span class="kpi-icon kpi-purple"><i class="bi bi-bar-chart-fill"></i></span>
                </div>
                <div class="kpi-value">{{ stats.conversion_rate }}%</div>
                <div class="kpi-diff" :class="stats.conversion_rate_diff >= 0 ? 'text-success' : 'text-danger'">
                  <i :class="stats.conversion_rate_diff >= 0 ? 'bi bi-arrow-up-short' : 'bi bi-arrow-down-short'"></i>
                  {{ Math.abs(stats.conversion_rate_diff) }}% vs 昨日
                </div>
                <svg :width="SVG_W" height="50" class="mt-2 kpi-sparkline">
                  <polyline :points="sparkPoints(stats.pv_by_hour.map((v,i) => Math.max(0, v - (stats?.uv_by_hour[i] ?? 0))), '#0d9b8e')" fill="none" stroke="#0d9b8e" stroke-width="1.5" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间：访客趋势 + 客户来源分析 -->
        <div class="row g-3 mb-4">
          <!-- 访客趋势折线图 -->
          <div class="col-12 col-xl-8">
            <div class="card h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <h6 class="card-title mb-0 fw-bold">访客趋势</h6>
                  <div class="d-flex gap-3 align-items-center">
                    <span class="d-flex align-items-center gap-1 small"><span style="width:12px;height:3px;background:#fd7e14;display:inline-block;border-radius:2px"></span> PV</span>
                    <span class="d-flex align-items-center gap-1 small"><span style="width:12px;height:3px;background:#20c997;display:inline-block;border-radius:2px"></span> UV</span>
                    <div class="btn-group ms-2">
                      <button class="btn btn-xs py-0 px-2 btn-warning btn-sm" style="font-size:12px">今日</button>
                      <button class="btn btn-xs py-0 px-2 btn-outline-secondary btn-sm" style="font-size:12px">昨日</button>
                      <button class="btn btn-xs py-0 px-2 btn-outline-secondary btn-sm" style="font-size:12px">近7日</button>
                    </div>
                  </div>
                </div>
                <svg viewBox="0 0 540 140" class="w-100" style="height:160px">
                  <!-- 网格线 -->
                  <line v-for="y in [20,50,80,110]" :key="y" x1="0" :y1="y" x2="540" :y2="y" stroke="#f0f0f0" stroke-width="1" />
                  <!-- 刻度标签 -->
                  <text v-for="(h, i) in [0,4,8,12,16,20,23]" :key="i" :x="(h/23)*540" y="138" text-anchor="middle" font-size="9" fill="#999">{{ String(h).padStart(2,'0') }}:00</text>
                  <!-- UV 面积填充 -->
                  <defs>
                    <linearGradient id="uvGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#20c997" stop-opacity="0.2" />
                      <stop offset="100%" stop-color="#20c997" stop-opacity="0" />
                    </linearGradient>
                  </defs>
                  <polygon :points="'0,130 ' + uvPoints + ' 540,130'" fill="url(#uvGrad)" />
                  <polyline :points="uvPoints" fill="none" stroke="#20c997" stroke-width="2" stroke-linejoin="round" />
                  <!-- PV 线 -->
                  <polyline :points="pvPoints" fill="none" stroke="#fd7e14" stroke-width="2" stroke-linejoin="round" />
                  <!-- PV 点 -->
                  <circle v-for="(v, i) in stats.pv_by_hour" :key="i" :cx="(i/23)*540" :cy="lineY(v, pvMax)" r="3" fill="#fd7e14" />
                </svg>
              </div>
            </div>
          </div>

          <!-- 客户来源分析 -->
          <div class="col-12 col-xl-4">
            <div class="card h-100">
              <div class="card-body">
                <h6 class="card-title fw-bold mb-3">客户来源分析</h6>
                <div v-for="source in stats.source_analysis" :key="source.label" class="mb-3">
                  <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="small">{{ source.label }}</span>
                    <span class="small fw-bold text-primary">{{ source.value }} <small class="text-secondary">{{ source.pct }}%</small></span>
                  </div>
                  <div class="progress" style="height:8px">
                    <div class="progress-bar" :style="{ width: source.pct + '%', background: '#0d6efd' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 下方：漏斗 + 热销商品 + 滞销商品 -->
        <div class="row g-3 mb-4">
          <!-- 下单漏斗 -->
          <div class="col-12 col-xl-3">
            <div class="card h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <h6 class="card-title fw-bold mb-0">下单漏斗</h6>
                  <i class="bi bi-arrow-counterclockwise text-secondary" style="cursor:pointer" title="刷新"></i>
                </div>
                <div v-for="step in stats.funnel" :key="step.label" class="mb-3">
                  <div class="d-flex justify-content-between small mb-1">
                    <span>{{ step.label }}</span>
                    <span class="fw-bold">{{ step.value }}</span>
                  </div>
                  <div class="progress" style="height:20px;border-radius:4px">
                    <div class="progress-bar" :style="{ width: Math.max(2, step.value / funnelMax * 100) + '%' }" :class="step.label === '商品浏览量' ? 'bg-warning' : step.label === '独立访客' ? 'bg-info' : 'bg-primary'"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热销商品 -->
          <div class="col-12 col-xl-4">
            <div class="card h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <h6 class="card-title fw-bold mb-0">热销商品</h6>
                  <i class="bi bi-funnel text-secondary" style="cursor:pointer"></i>
                </div>
                <div v-if="!stats.hot_products.length" class="text-center py-4 text-secondary small">暂无销售数据</div>
                <div v-for="product in stats.hot_products" :key="product.product_id" class="d-flex align-items-center gap-2 mb-3">
                  <span class="text-warning fs-5"><i class="bi bi-star-fill"></i></span>
                  <img v-if="product.cover" :src="product.cover" class="rounded" style="width:40px;height:40px;object-fit:cover" :alt="product.title" />
                  <div class="flex-grow-1 overflow-hidden">
                    <div class="text-truncate small fw-semibold">{{ product.title }}</div>
                    <div class="progress mt-1" style="height:4px"><div class="progress-bar bg-warning" style="width:0%"></div></div>
                  </div>
                  <div class="text-end">
                    <div class="small fw-bold text-dark">\${{ product.price.toFixed(2) }}</div>
                    <div class="text-secondary" style="font-size:11px">销量: {{ product.quantity }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 滞销商品 -->
          <div class="col-12 col-xl-4">
            <div class="card h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <h6 class="card-title fw-bold mb-0">滞销商品</h6>
                  <i class="bi bi-funnel text-secondary" style="cursor:pointer"></i>
                </div>
                <div v-if="!stats.slow_products.length" class="text-center py-4 text-secondary small">暂无数据</div>
                <div v-for="product in stats.slow_products" :key="product.product_id" class="d-flex align-items-center gap-2 mb-3">
                  <span class="text-danger fs-5"><i class="bi bi-dash-circle-fill"></i></span>
                  <img v-if="product.cover" :src="product.cover" class="rounded" style="width:40px;height:40px;object-fit:cover" :alt="product.title" />
                  <div class="flex-grow-1 overflow-hidden">
                    <div class="text-truncate small fw-semibold">{{ product.title }}</div>
                    <div class="progress mt-1" style="height:4px"><div class="progress-bar bg-danger" style="width:5%"></div></div>
                  </div>
                  <div class="text-end">
                    <div class="small fw-bold text-dark">\${{ product.price.toFixed(2) }}</div>
                    <div class="text-secondary" style="font-size:11px">销量: {{ product.quantity }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="row g-3">
          <div class="col-6 col-md-3">
            <div class="card text-center py-3 dash-quick-card" @click="navigate('/admin/orders')">
              <i class="bi bi-clipboard-check fs-3 text-primary mb-1"></i>
              <div class="small fw-semibold">{{ stats.recent_orders_count }} 笔订单</div>
              <div class="text-secondary" style="font-size:11px">点击查看</div>
            </div>
          </div>
          <div class="col-6 col-md-3">
            <div class="card text-center py-3 dash-quick-card" @click="navigate('/admin/tickets')">
              <i class="bi bi-headset fs-3 text-warning mb-1"></i>
              <div class="small fw-semibold">{{ stats.recent_tickets_open }} 待处理工单</div>
              <div class="text-secondary" style="font-size:11px">共 {{ stats.recent_tickets_total }} 个</div>
            </div>
          </div>
          <div class="col-6 col-md-3">
            <div class="card text-center py-3 dash-quick-card" @click="navigate('/admin/products')">
              <i class="bi bi-box-seam fs-3 text-success mb-1"></i>
              <div class="small fw-semibold">商品管理</div>
              <div class="text-secondary" style="font-size:11px">查看库存</div>
            </div>
          </div>
          <div class="col-6 col-md-3">
            <div class="card text-center py-3 dash-quick-card" @click="navigate('/admin/ai-monitor')">
              <i class="bi bi-cpu fs-3 text-info mb-1"></i>
              <div class="small fw-semibold">AI 监控</div>
              <div class="text-secondary" style="font-size:11px">查看对话分析</div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </AdminShell>
</template>

<style scoped>
.dash-kpi-card { border: none; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
.kpi-value { font-size: 2rem; font-weight: 700; line-height: 1.1; }
.kpi-diff { font-size: 12px; margin-top: 2px; }
.kpi-icon { display:flex; align-items:center; justify-content:center; width:36px; height:36px; border-radius:8px; font-size:16px; }
.kpi-orange { background: #fff3e0; color: #fd7e14; }
.kpi-blue { background: #e7f0ff; color: #0d6efd; }
.kpi-green { background: #e8f5e9; color: #198754; }
.kpi-purple { background: #f3e5f5; color: #6f42c1; }
.kpi-sparkline { overflow: visible; }
.dash-quick-card { cursor: pointer; transition: box-shadow .2s; border: 1px solid #f0f0f0; }
.dash-quick-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.12); }
</style>
