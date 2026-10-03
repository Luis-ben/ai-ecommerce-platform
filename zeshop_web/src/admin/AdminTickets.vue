<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { navigate } from '../router'
import { isStaff, notify } from '../store'

type AdminTicket = {
  ticket_id: string
  customer_id: string
  customer_name?: string
  customer_email?: string
  customer_phone?: string
  order_id: string | null
  product_id?: string
  product_title?: string
  sku?: string
  quantity?: number
  intent: string
  status: string
  priority: string
  created_at: string
}

const tickets = ref<AdminTicket[]>([])
const loading = ref(true)
const filterStatus = ref('')
const filterKeyword = ref('')

const statusLabel: Record<string, string> = {
  OPEN: '待处理',
  IN_PROGRESS: '已批准待顾客寄回商品',
  SHIPPED_BACK: '已发货寄回商品',
  CLOSED: '已完成',
}

const statusBadgeClass: Record<string, string> = {
  OPEN: 'badge-status-orange',
  IN_PROGRESS: 'badge-status-blue',
  SHIPPED_BACK: 'badge-status-purple',
  CLOSED: 'badge-status-green',
}

const intentLabel: Record<string, string> = {
  RETURN_REQUEST: '退货',
  EXCHANGE_REQUEST: '换货',
  COMPLAINT: '投诉',
}

const filtered = computed(() => tickets.value.filter((item) => {
  if (filterStatus.value && item.status !== filterStatus.value) return false
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    const hit = [item.customer_name, item.customer_email, item.customer_phone, item.product_title, item.sku, item.ticket_id]
      .filter(Boolean)
      .some(s => s!.toLowerCase().includes(kw))
    if (!hit) return false
  }
  return true
}))

const openCount = computed(() => tickets.value.filter((item) => item.status !== 'CLOSED').length)

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<{ items: AdminTicket[] }>('/api/support/tickets')
    tickets.value = data.items
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<!-- 对齐 BeikeShop 官方后台 售后管理（pages/rmas/index.blade.php） -->
<template>
  <AdminShell active="orders" active-child="/admin/tickets" title="售后管理">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要客服或商户角色，请用 agent@example.com 登录。</p>
    <template v-else>
      <div class="card h-min-600">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">售后工单列表（共 {{ filtered.length }} 条，未关闭 {{ openCount }} 条）</span>
          </div>

          <!-- 筛选栏 -->
          <div class="bg-light rounded-3 p-4 mb-4">
            <div class="row g-3 align-items-end">
              <div class="col-12 col-md-4">
                <label class="filter-title">关键词搜索</label>
                <input v-model="filterKeyword" class="form-control" placeholder="姓名 / 邮箱 / 电话 / 商品 / SKU" />
              </div>
              <div class="col-12 col-md-3">
                <label class="filter-title">状态</label>
                <select v-model="filterStatus" class="form-select">
                  <option value="">全部</option>
                  <option value="OPEN">待处理</option>
                  <option value="IN_PROGRESS">已批准待顾客寄回商品</option>
                  <option value="CLOSED">已完成</option>
                </select>
              </div>
              <div class="col-auto">
                <button type="button" class="btn btn-primary btn-sm me-2" @click="load()">筛选</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="filterStatus = ''; filterKeyword = ''">重置</button>
              </div>
            </div>
          </div>

          <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
          <div v-else-if="filtered.length" class="table-push">
            <table class="table table-hover align-middle">
              <thead>
                <tr>
                  <th>客户姓名</th>
                  <th>邮箱</th>
                  <th>电话</th>
                  <th style="min-width: 220px; max-width: 320px">商品</th>
                  <th>SKU</th>
                  <th>数量</th>
                  <th>服务类型</th>
                  <th>状态</th>
                  <th>创建时间</th>
                  <th class="text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="ticket in filtered"
                  :key="ticket.ticket_id"
                  class="cursor-pointer row-link"
                  @click="navigate('/admin/tickets/' + ticket.ticket_id)"
                >
                  <td class="fw-medium">{{ ticket.customer_name || ticket.customer_id }}</td>
                  <td class="text-secondary small">{{ ticket.customer_email || '-' }}</td>
                  <td class="text-secondary small">{{ ticket.customer_phone || '-' }}</td>
                  <td class="text-truncate" style="max-width: 300px" :title="ticket.product_title || ''">
                    <span class="small">{{ ticket.product_title || ticket.order_id || '—' }}</span>
                  </td>
                  <td class="font-monospace small text-secondary">{{ ticket.sku || '—' }}</td>
                  <td class="fw-semibold">{{ ticket.quantity || 1 }}</td>
                  <td>
                    <span class="badge bg-light text-dark border">
                      {{ intentLabel[ticket.intent] || ticket.intent }}
                    </span>
                  </td>
                  <td>
                    <span class="rma-status-badge" :class="statusBadgeClass[ticket.status] || 'badge-status-blue'">
                      {{ statusLabel[ticket.status] || ticket.status }}
                    </span>
                  </td>
                  <td class="text-nowrap small text-secondary">
                    {{ ticket.created_at.replace('T', ' ').slice(0, 19) }}
                  </td>
                  <td class="text-center text-nowrap" @click.stop>
                    <button
                      class="btn btn-outline-primary btn-sm"
                      type="button"
                      @click="navigate('/admin/tickets/' + ticket.ticket_id)"
                    >
                      处理
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-secondary">
            <i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">当前筛选下没有工单</p>
          </div>
        </div>
      </div>
    </template>
  </AdminShell>
</template>

<style scoped>
.rma-status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.badge-status-blue {
  background: #e8f0fe;
  color: #1a73e8;
}
.badge-status-orange {
  background: #fef3e8;
  color: #e8731a;
}
.badge-status-purple {
  background: #f3e8ff;
  color: #7e22ce;
}
.badge-status-green {
  background: #e6f4ea;
  color: #137333;
}
</style>
