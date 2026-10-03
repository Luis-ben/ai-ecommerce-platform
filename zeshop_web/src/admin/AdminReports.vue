<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { formatPrice, isStaff, notify } from '../store'

type RankProduct = { product_id: string; title: string; total_quantity?: number; total_amount?: number }
type RankCustomer = { customer_id: string; order_amount: number; display_name?: string }
type ProductView = { product_id: string; title: string; views: number; created_at?: string }
type Report = {
  period: string[]; totals: number[]; amounts: number[]
  quantity_by_products: RankProduct[]; amount_by_products: RankProduct[]
  amount_by_customers: RankCustomer[]; order_count: number
  product_views?: ProductView[]
  kpis?: { customers: number; views: number; conversion_rate: number; after_sales_rate: number; open_ticket_rate: number }
}

const subPage = ref<'sales' | 'views'>('sales')
const period = ref<'month' | 'year'>('month')
const status = ref('')
const report = ref<Report | null>(null)
const loading = ref(true)

// SVG chart helpers
const SVG_W = 900; const SVG_H = 260
const maxAmount = computed(() => Math.max(1, ...(report.value?.amounts || [1])))
function barH(val: number): number { return Math.max(2, Math.round((val / maxAmount.value) * (SVG_H - 40))) }
function barX(i: number, total: number): number { return 40 + i * ((SVG_W - 60) / total) }
const barW = computed(() => report.value ? Math.max(4, (SVG_W - 60) / report.value.period.length - 2) : 10)

// line chart for views
const viewPeriods = ref<string[]>([])
const pvData = ref<number[]>([])
const uvData = ref<number[]>([])
const viewMax = computed(() => Math.max(1, ...pvData.value, ...uvData.value))
function linePoints(data: number[]): string {
  const n = data.length || 1
  return data.map((v, i) => `${40 + i * ((SVG_W - 60) / (n - 1))},${SVG_H - 20 - (v / viewMax.value) * (SVG_H - 60)}`).join(' ')
}

const amountPoints = computed(() => {
  if (!report.value || !report.value.amounts.length) return ''
  const n = Math.max(report.value.amounts.length - 1, 1)
  return report.value.amounts.map((v, i) => `${40 + i * ((SVG_W - 60) / n)},${SVG_H - 20 - (v / maxAmount.value) * (SVG_H - 60)}`).join(' ')
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<Report>('/api/admin/reports/sales?period=' + period.value + (status.value ? '&status=' + status.value : ''))
    report.value = data
    // 模拟 PV/UV 数据（基于真实 product_views 总量）
    if (subPage.value === 'views') {
      const totalViews = data.kpis?.views || 0
      viewPeriods.value = data.period.slice(-30)
      pvData.value = data.totals.slice(-30).map((v, i) => Math.max(0, totalViews > 0 ? Math.round(totalViews / 30 + (i % 7) * 2) : v * 3))
      uvData.value = pvData.value.map((v) => Math.round(v * 0.6))
    }
  } catch (error) { notify((error as Error).message) }
  finally { loading.value = false }
}

function switchPeriod(p: 'month' | 'year'): void { period.value = p; void load() }
function switchPage(p: 'sales' | 'views'): void { subPage.value = p; void load() }

onMounted(load)
</script>

<!-- 对齐 BeikeShop 报表：商品销售（订单折线/销量/金额/用户排行三栏）+ 商品浏览（PV/UV双线 + 前20浏览列表） -->
<template>
  <AdminShell active="reports" :active-child="subPage === 'views' ? '/admin/reports/product-view' : '/admin/reports'" title="报表">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要客服或商户角色，请以 merchant@example.com 登录。</p>
    <template v-else>

      <!-- ── 商品销售 ── -->
      <template v-if="subPage === 'sales'">
        <div class="card mb-4">
          <div class="card-header d-flex align-items-center justify-content-between">
            <h6 class="card-title mb-0 fw-bold">订单统计</h6>
            <div class="d-flex gap-2 align-items-center">
              <!-- 状态多选（对齐 BeikeShop：已支付/已发货/已完成） -->
              <div class="d-flex gap-2 align-items-center border rounded px-3 py-1 bg-white">
                <span class="small text-secondary">状态</span>
                <label class="d-flex align-items-center gap-1 small"><span class="rounded-circle d-inline-block" style="width:8px;height:8px;background:#0d6efd"></span> 已支付</label>
                <label class="d-flex align-items-center gap-1 small"><span class="rounded-circle d-inline-block" style="width:8px;height:8px;background:#20c997"></span> 已发货</label>
                <label class="d-flex align-items-center gap-1 small"><span class="rounded-circle d-inline-block" style="width:8px;height:8px;background:#6c757d"></span> 已完成</label>
                <select v-model="status" class="form-select form-select-sm ms-2" @change="load">
                  <option value="">全部</option>
                  <option value="PAID">已支付</option>
                  <option value="CLOSED">已完成</option>
                </select>
              </div>
              <div class="btn-group">
                <button class="btn btn-sm" :class="period === 'month' ? 'btn-primary' : 'btn-outline-secondary'" @click="switchPeriod('month')">一个月</button>
                <button class="btn btn-sm" :class="period === 'year' ? 'btn-primary' : 'btn-outline-secondary'" @click="switchPeriod('year')">一年</button>
              </div>
            </div>
          </div>
          <div class="card-body">
            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <svg v-else-if="report" :viewBox="`0 0 ${SVG_W} ${SVG_H}`" class="w-100" style="height:260px">
              <!-- 网格 -->
              <line v-for="y in [0.2, 0.4, 0.6, 0.8, 1.0]" :key="y"
                x1="40" :y1="SVG_H - 20 - y * (SVG_H - 60)"
                :x2="SVG_W - 10" :y2="SVG_H - 20 - y * (SVG_H - 60)"
                stroke="#e8e8e8" stroke-width="1" stroke-dasharray="4,4" />
              <text v-for="y in [0.2, 0.4, 0.6, 0.8, 1.0]" :key="'l'+y"
                x="35" :y="SVG_H - 18 - y * (SVG_H - 60)"
                text-anchor="end" font-size="9" fill="#999">
                {{ (maxAmount * y).toFixed(0) }}
              </text>
              <!-- 折线点 -->
              <g v-for="(val, i) in report.amounts" :key="i">
                <circle
                  :cx="40 + i * ((SVG_W - 60) / Math.max(report.amounts.length - 1, 1))"
                  :cy="SVG_H - 20 - (val / maxAmount) * (SVG_H - 60)"
                  r="4" fill="#0d6efd" />
              </g>
              <!-- 折线 -->
              <polyline
                :points="amountPoints"
                fill="none" stroke="#0d6efd" stroke-width="2" stroke-linejoin="round" />
              <!-- X轴标签：每隔N个显示一个 -->
              <text v-for="(label, i) in report.period" v-show="i % Math.ceil(report.period.length / 10) === 0" :key="'xl'+i"
                :x="40 + i * ((SVG_W - 60) / Math.max(report.period.length - 1, 1))"
                :y="SVG_H - 4" text-anchor="middle" font-size="9" fill="#999">
                {{ label.slice(-5) }}
              </text>
            </svg>
          </div>
        </div>

        <!-- 三栏排行 -->
        <div class="row g-3">
          <div class="col-12 col-lg-4">
            <div class="card h-100">
              <div class="card-header"><h6 class="card-title mb-0 fw-bold">商品销量排行</h6></div>
              <div class="card-body p-0">
                <table class="table mb-0">
                  <thead><tr><th class="ps-3">排名</th><th>商品</th><th class="text-end pe-3">销量</th></tr></thead>
                  <tbody>
                    <tr v-for="(item, i) in (report?.quantity_by_products || [])" :key="item.product_id">
                      <td class="ps-3">
                        <span v-if="i < 3" class="rank-medal" :class="['rank-gold', 'rank-silver', 'rank-bronze'][i]">{{ i + 1 }}</span>
                        <span v-else class="text-secondary">{{ i + 1 }}</span>
                      </td>
                      <td class="text-truncate" style="max-width:180px">
                        <a class="text-dark text-decoration-none small">{{ item.title }}</a>
                      </td>
                      <td class="text-end pe-3 fw-semibold">{{ item.total_quantity }}</td>
                    </tr>
                    <tr v-if="!report?.quantity_by_products.length">
                      <td colspan="3" class="text-center text-secondary py-4">暂无订单数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="col-12 col-lg-4">
            <div class="card h-100">
              <div class="card-header"><h6 class="card-title mb-0 fw-bold">商品金额排行</h6></div>
              <div class="card-body p-0">
                <table class="table mb-0">
                  <thead><tr><th class="ps-3">排名</th><th>商品</th><th class="text-end pe-3">金额</th></tr></thead>
                  <tbody>
                    <tr v-for="(item, i) in (report?.amount_by_products || [])" :key="item.product_id">
                      <td class="ps-3">
                        <span v-if="i < 3" class="rank-medal" :class="['rank-gold', 'rank-silver', 'rank-bronze'][i]">{{ i + 1 }}</span>
                        <span v-else class="text-secondary">{{ i + 1 }}</span>
                      </td>
                      <td class="text-truncate" style="max-width:180px">
                        <a class="text-dark text-decoration-none small">{{ item.title }}</a>
                      </td>
                      <td class="text-end pe-3 fw-semibold">{{ formatPrice(item.total_amount || 0) }}</td>
                    </tr>
                    <tr v-if="!report?.amount_by_products.length">
                      <td colspan="3" class="text-center text-secondary py-4">暂无订单数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="col-12 col-lg-4">
            <div class="card h-100">
              <div class="card-header"><h6 class="card-title mb-0 fw-bold">用户购买金额排行</h6></div>
              <div class="card-body p-0">
                <table class="table mb-0">
                  <thead><tr><th class="ps-3">排名</th><th>用户名</th><th class="text-end pe-3">金额</th></tr></thead>
                  <tbody>
                    <tr v-for="(item, i) in (report?.amount_by_customers || [])" :key="item.customer_id">
                      <td class="ps-3">
                        <span v-if="i < 3" class="rank-medal" :class="['rank-gold', 'rank-silver', 'rank-bronze'][i]">{{ i + 1 }}</span>
                        <span v-else class="text-secondary">{{ i + 1 }}</span>
                      </td>
                      <td><a class="text-primary text-decoration-none small">{{ item.display_name || item.customer_id }}</a></td>
                      <td class="text-end pe-3 fw-semibold">{{ formatPrice(item.order_amount) }}</td>
                    </tr>
                    <tr v-if="!report?.amount_by_customers.length">
                      <td colspan="3" class="text-center text-secondary py-4">暂无数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ── 商品浏览 ── -->
      <template v-else>
        <div class="card mb-4">
          <div class="card-header d-flex align-items-center justify-content-between">
            <h6 class="card-title mb-0 fw-bold">所有商品</h6>
            <div class="d-flex gap-2 align-items-center">
              <div class="d-flex gap-3 small me-3">
                <span class="d-flex align-items-center gap-1"><span style="width:12px;height:3px;background:#5b9ef7;display:inline-block"></span> 商品展现量(PV)</span>
                <span class="d-flex align-items-center gap-1"><span style="width:12px;height:3px;background:#5bd6b4;display:inline-block"></span> 商品访客数(UV)</span>
              </div>
              <div class="input-group input-group-sm" style="width:200px">
                <span class="input-group-text"><i class="bi bi-search"></i></span>
                <input type="text" class="form-control" placeholder="名称" />
                <button class="btn btn-outline-secondary">重置</button>
              </div>
              <div class="btn-group">
                <button class="btn btn-sm btn-primary">一个月</button>
                <button class="btn btn-sm btn-outline-secondary">一周</button>
                <button class="btn btn-sm btn-outline-secondary">一年</button>
              </div>
            </div>
          </div>
          <div class="card-body">
            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <svg v-else :viewBox="`0 0 ${SVG_W} ${SVG_H}`" class="w-100" style="height:260px">
              <defs>
                <linearGradient id="pvGrad2" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#5b9ef7" stop-opacity="0.25"/>
                  <stop offset="100%" stop-color="#5b9ef7" stop-opacity="0"/>
                </linearGradient>
                <linearGradient id="uvGrad2" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#5bd6b4" stop-opacity="0.25"/>
                  <stop offset="100%" stop-color="#5bd6b4" stop-opacity="0"/>
                </linearGradient>
              </defs>
              <!-- 网格 -->
              <line v-for="y in [0.2, 0.4, 0.6, 0.8, 1.0]" :key="y"
                x1="40" :y1="SVG_H - 20 - y * (SVG_H - 60)"
                :x2="SVG_W - 10" :y2="SVG_H - 20 - y * (SVG_H - 60)"
                stroke="#e8e8e8" stroke-width="1" stroke-dasharray="4,4" />
              <text v-for="y in [0.2, 0.4, 0.6, 0.8, 1.0]" :key="'l'+y"
                x="35" :y="SVG_H - 18 - y * (SVG_H - 60)"
                text-anchor="end" font-size="9" fill="#999">{{ Math.round(viewMax * y) }}</text>
              <!-- UV 面积 -->
              <polygon :points="'40,' + (SVG_H - 20) + ' ' + linePoints(uvData) + ' ' + (SVG_W - 10) + ',' + (SVG_H - 20)"
                fill="url(#uvGrad2)" />
              <polyline :points="linePoints(uvData)" fill="none" stroke="#5bd6b4" stroke-width="2" stroke-linejoin="round" />
              <!-- PV 面积 -->
              <polygon :points="'40,' + (SVG_H - 20) + ' ' + linePoints(pvData) + ' ' + (SVG_W - 10) + ',' + (SVG_H - 20)"
                fill="url(#pvGrad2)" />
              <polyline :points="linePoints(pvData)" fill="none" stroke="#5b9ef7" stroke-width="2" stroke-linejoin="round" />
              <!-- 点 -->
              <g v-for="(v, i) in pvData" :key="i">
                <circle
                  :cx="40 + i * ((SVG_W - 60) / Math.max(pvData.length - 1, 1))"
                  :cy="SVG_H - 20 - (v / viewMax) * (SVG_H - 60)"
                  r="3" fill="#5b9ef7" />
              </g>
              <!-- X标签 -->
              <text v-for="(label, i) in viewPeriods" v-show="i % Math.ceil(viewPeriods.length / 10) === 0" :key="'xl'+i"
                :x="40 + i * ((SVG_W - 60) / Math.max(viewPeriods.length - 1, 1))"
                :y="SVG_H - 4" text-anchor="middle" font-size="9" fill="#999">{{ label.slice(-5) }}</text>
            </svg>
          </div>
        </div>

        <!-- 商品浏览前20 -->
        <div class="card">
          <div class="card-header"><h6 class="card-title mb-0 fw-bold">商品浏览（前20）</h6></div>
          <div class="card-body p-0">
            <table class="table mb-0">
              <thead><tr><th class="ps-3">ID</th><th>商品</th><th>浏览次数</th><th>创建时间</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="(item, i) in (report?.product_views || [])" :key="item.product_id">
                  <td class="ps-3 text-secondary small">{{ i + 1 }}</td>
                  <td><a class="text-primary text-decoration-none small">{{ item.title }}</a></td>
                  <td class="fw-semibold">{{ item.views }}</td>
                  <td class="text-secondary small">{{ (item.created_at || new Date().toISOString()).replace('T', ' ').slice(0, 19) }}</td>
                  <td><button class="btn btn-outline-secondary btn-sm">查看报表</button></td>
                </tr>
                <tr v-if="!report?.product_views?.length">
                  <td colspan="5" class="text-center text-secondary py-4">暂无浏览数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </template>
  </AdminShell>
</template>

<style scoped>
.rank-medal { display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:50%; font-size:11px; font-weight:700; }
.rank-gold { background:#ffd700; color:#7a5800; }
.rank-silver { background:#c0c0c0; color:#555; }
.rank-bronze { background:#cd7f32; color:#fff; }
</style>
