<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { formatPrice, imageOf, isStaff, notify } from '../store'

type AdminOrder = {
  id: string; customer_id: string; status: string; total: number
  shipping_status: string; created_at: string; updated_at?: string
  payment_method?: string
  items: { product_id: string; title: string; quantity: number; unit_price: number }[]
}

const orders = ref<AdminOrder[]>([])
const loading = ref(true)
const expanded = ref('')

// 筛选字段（对齐BeikeShop：订单号/客户姓名/Email/状态/时间范围）
const filterOrderNo = ref('')
const filterCustomer = ref('')
const filterEmail = ref('')
const filterStatus = ref('')
const filterDateStart = ref('')
const filterDateEnd = ref('')

const statusLabel: Record<string, string> = { PAID: '已付款', PENDING: '待付款', CLOSED: '已关闭', REFUNDING: '退款中' }
const statusBadge: Record<string, string> = { PAID: 'badge-status-blue', PENDING: 'badge-status-orange', CLOSED: 'badge-status-gray', REFUNDING: 'badge-status-red' }
const shippingLabel: Record<string, string> = { PROCESSING: '待发货', SHIPPED: '待收货', DELIVERED: '已完成', IN_TRANSIT: '运输中' }

const editing = ref<Record<string, { status: string; shipping_status: string }>>({})

const filtered = computed(() => orders.value.filter((o) => {
  if (filterOrderNo.value && !o.id.includes(filterOrderNo.value)) return false
  if (filterCustomer.value && !o.customer_id.toLowerCase().includes(filterCustomer.value.toLowerCase())) return false
  if (filterStatus.value && o.status !== filterStatus.value) return false
  return true
}))

const revenue = computed(() => filtered.value.reduce((s, o) => s + o.total, 0))

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<{ items: AdminOrder[] }>('/api/admin/orders')
    orders.value = data.items
    editing.value = Object.fromEntries(data.items.map((o) => [o.id, { status: o.status, shipping_status: o.shipping_status }]))
  } catch (error) { notify((error as Error).message) }
  finally { loading.value = false }
}

async function updateOrder(order: AdminOrder): Promise<void> {
  try {
    await api('/api/admin/orders/' + order.id, { method: 'PATCH', body: JSON.stringify(editing.value[order.id]) })
    notify('订单状态已保存'); await load()
  } catch (error) { notify((error as Error).message) }
}

async function deleteOrder(id: string): Promise<void> {
  if (!confirm('确定删除该订单？')) return
  try { await api('/api/admin/orders/' + id, { method: 'DELETE' }); notify('已删除'); await load() }
  catch (error) { notify((error as Error).message) }
}

function resetFilter(): void {
  filterOrderNo.value = ''; filterCustomer.value = ''; filterEmail.value = ''
  filterStatus.value = ''; filterDateStart.value = ''; filterDateEnd.value = ''
}

onMounted(load)
</script>

<!-- 对齐 BeikeShop 订单列表：筛选栏+导出+创建时间范围+Stripe/PayPal支付方式+状态badge+删除操作 -->
<template>
  <AdminShell active="orders" active-child="/admin/orders" title="订单列表">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请以 merchant@example.com 登录。</p>
    <template v-else>
      <div class="card h-min-600">
        <div class="card-body">
          <!-- 筛选栏 -->
          <div class="bg-light rounded-3 p-4 mb-4">
            <div class="row g-3 align-items-end">
              <div class="col-12 col-md-3">
                <label class="filter-title">订单号</label>
                <input v-model="filterOrderNo" class="form-control" placeholder="订单号" @keyup.enter="load" />
              </div>
              <div class="col-12 col-md-3">
                <label class="filter-title">客户姓名</label>
                <input v-model="filterCustomer" class="form-control" placeholder="客户姓名" />
              </div>
              <div class="col-12 col-md-3">
                <label class="filter-title">Email</label>
                <input v-model="filterEmail" class="form-control" placeholder="Email" />
              </div>
              <div class="col-12 col-md-2">
                <label class="filter-title">状态</label>
                <select v-model="filterStatus" class="form-select">
                  <option value="">全部</option>
                  <option value="PAID">已付款</option>
                  <option value="PENDING">待付款</option>
                  <option value="CLOSED">已关闭</option>
                  <option value="REFUNDING">退款中</option>
                </select>
              </div>
            </div>
            <div class="row g-3 mt-1 align-items-end">
              <div class="col-12 col-md-5">
                <label class="filter-title">创建时间</label>
                <div class="d-flex gap-2 align-items-center">
                  <input v-model="filterDateStart" type="date" class="form-control" />
                  <span class="text-secondary">-</span>
                  <input v-model="filterDateEnd" type="date" class="form-control" />
                </div>
              </div>
              <div class="col-auto">
                <button type="button" class="btn btn-primary btn-sm me-2" @click="load">筛选</button>
                <button type="button" class="btn btn-outline-secondary btn-sm me-2" @click="resetFilter">重置</button>
                <button type="button" class="btn btn-outline-secondary btn-sm"><i class="bi bi-download me-1"></i>导出</button>
              </div>
            </div>
          </div>

          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">共 {{ filtered.length }} 笔，合计 {{ formatPrice(revenue) }}</span>
          </div>

          <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
          <div v-else-if="filtered.length" class="table-push">
            <table class="table table-hover align-middle">
              <thead>
                <tr>
                  <th><input type="checkbox" /></th>
                  <th>ID</th>
                  <th>订单号</th>
                  <th>客户姓名</th>
                  <th>支付方式</th>
                  <th>状态</th>
                  <th>订单总额</th>
                  <th>创建时间</th>
                  <th>更新时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="order in filtered" :key="order.id">
                  <tr class="cursor-pointer row-link" @click="expanded = expanded === order.id ? '' : order.id">
                    <td @click.stop><input type="checkbox" /></td>
                    <td class="text-secondary small">{{ filtered.indexOf(order) + 1 }}</td>
                    <td class="font-monospace small">{{ order.id.slice(-12) }}</td>
                    <td>{{ order.customer_id }}</td>
                    <td>{{ order.payment_method || 'PayPal' }}</td>
                    <td>
                      <span class="order-status-badge" :class="statusBadge[order.status] || 'badge-status-gray'">
                        {{ statusLabel[order.status] || order.status }}
                      </span>
                    </td>
                    <td class="fw-semibold">{{ formatPrice(order.total) }}</td>
                    <td class="text-nowrap small">{{ order.created_at.replace('T', ' ').slice(0, 19) }}</td>
                    <td class="text-nowrap small">{{ (order.updated_at || order.created_at).replace('T', ' ').slice(0, 19) }}</td>
                    <td @click.stop>
                      <button class="btn btn-outline-danger btn-sm" type="button" @click="deleteOrder(order.id)">删除</button>
                    </td>
                  </tr>
                  <!-- 展开：修改状态 + 商品明细 -->
                  <tr v-if="expanded === order.id">
                    <td colspan="10" class="bg-light p-3">
                      <div class="row g-3 mb-3">
                        <div class="col-auto">
                          <label class="small fw-semibold me-2">支付状态</label>
                          <select v-model="editing[order.id].status" class="form-select form-select-sm d-inline-block" style="width:auto">
                            <option value="PAID">已付款</option>
                            <option value="PENDING">待付款</option>
                            <option value="CLOSED">已关闭</option>
                          </select>
                        </div>
                        <div class="col-auto">
                          <label class="small fw-semibold me-2">物流状态</label>
                          <select v-model="editing[order.id].shipping_status" class="form-select form-select-sm d-inline-block" style="width:auto">
                            <option value="PROCESSING">待发货</option>
                            <option value="IN_TRANSIT">运输中</option>
                            <option value="SHIPPED">待收货</option>
                            <option value="DELIVERED">已完成</option>
                          </select>
                        </div>
                        <div class="col-auto">
                          <button class="btn btn-primary btn-sm" type="button" @click="updateOrder(order)">保存</button>
                        </div>
                      </div>
                      <div v-for="item in order.items" :key="item.product_id" class="d-flex align-items-center gap-3 py-2 border-bottom">
                        <img :src="imageOf(item.product_id)" :alt="item.title" style="width:44px;height:44px;object-fit:cover;border-radius:6px;border:1px solid #eee" />
                        <span class="flex-grow-1">{{ item.title }} × {{ item.quantity }}</span>
                        <span class="fw-semibold">{{ formatPrice(item.unit_price * item.quantity) }}</span>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
          <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-secondary">
            <i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">当前筛选下没有订单</p>
          </div>
        </div>
      </div>
    </template>
  </AdminShell>
</template>

<style scoped>
.order-status-badge { display:inline-block; padding:3px 10px; border-radius:20px; font-size:12px; font-weight:500; }
.badge-status-blue { background:#e8f0fe; color:#1a73e8; }
.badge-status-orange { background:#fef3e8; color:#e8731a; }
.badge-status-gray { background:#f0f0f0; color:#888; }
.badge-status-red { background:#fde8e8; color:#e81a1a; }
</style>
