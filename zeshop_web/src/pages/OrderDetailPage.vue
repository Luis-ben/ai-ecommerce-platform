<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AccountSidebar from '../components/AccountSidebar.vue'
import { api } from '../api'
import { navigate } from '../router'
import { createTicket, formatPrice, imageOf, notify, state, type Order } from '../store'

const props = defineProps<{ id: string }>()
const order = ref<Order | null>(null)
const shipping = ref('')
const loading = ref(true)
const statusLabel: Record<string, string> = { PAID: '已付款', PENDING: '待付款', CLOSED: '已关闭' }
const shippingLabel: Record<string, string> = { PROCESSING: '待发货', SHIPPED: '待收货', DELIVERED: '已完成', IN_TRANSIT: '运输中' }
const itemTotal = computed(() => order.value?.items.reduce((sum, item) => sum + item.unit_price * item.quantity, 0) ?? 0)

onMounted(async () => {
  try {
    order.value = await api<Order>('/api/orders/' + encodeURIComponent(props.id))
    const result = await api<{ status: string }>('/api/orders/' + encodeURIComponent(props.id) + '/shipping')
    shipping.value = result.status
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
})

function askAi(): void {
  state.chatOpen = true
  notify('已打开 AI 客服，可以问这笔订单的物流')
}
function requestReturn(): void {
  createTicket('RETURN_REQUEST', props.id)
}
</script>

<!-- 结构对齐 BeikeShop 主题 shared/order_info.blade.php -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><a @click="navigate('/orders')">我的订单</a><span class="px-2">/</span><span class="text-dark">订单详情</span></nav></div>
  </div>

  <div class="container">
    <div class="row">
      <AccountSidebar active="orders" />
      <div class="col-12 col-md-9">
        <div v-if="loading" class="card"><div class="card-body text-center py-5 text-muted">加载中…</div></div>
        <div v-else-if="!order" class="card"><div class="card-body text-center py-5"><p class="text-muted">订单不存在或不属于当前账号</p><button class="btn btn-dark" @click="navigate('/orders')">返回订单列表</button></div></div>
        <template v-else>
          <div class="card mb-lg-4 mb-2 order-head">
            <div class="card-header d-flex align-items-center justify-content-between">
              <h6 class="card-title">订单详情</h6>
              <div>
                <button class="btn btn-primary btn-sm nowrap" type="button" @click="askAi">问 AI 物流</button>
                <button class="btn btn-outline-secondary btn-sm nowrap" type="button" @click="requestReturn">申请退货</button>
              </div>
            </div>
            <div class="card-body">
              <div class="bg-light p-2 table-responsive rounded-2">
                <table class="table table-borderless mb-0">
                  <thead><tr><th class="nowrap">订单号</th><th class="nowrap">下单时间</th><th class="nowrap">状态</th><th class="nowrap">订单金额</th><th class="nowrap">支付方式</th><th class="nowrap">配送方式</th></tr></thead>
                  <tbody>
                    <tr>
                      <td>{{ order.id }}</td>
                      <td class="nowrap">{{ order.created_at.replace('T', ' ').slice(0, 19) }}</td>
                      <td class="nowrap"><span class="text-success">{{ statusLabel[order.status] || order.status }}</span></td>
                      <td>{{ formatPrice(order.total) }}</td>
                      <td>模拟支付（SIMULATED）</td>
                      <td>{{ shippingLabel[shipping] || shipping || '—' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="card mb-lg-4 mb-2">
            <div class="card-header"><h6 class="card-title">收货信息</h6></div>
            <div class="card-body"><p class="text-muted mb-0">演示后端以下单时的服务端购物车生成订单，不保存收货地址与收货人；下单账号为 {{ state.user?.display_name }}。</p></div>
          </div>

          <div class="card mb-lg-4 mb-2">
            <div class="card-header"><h6 class="card-title">订单商品</h6></div>
            <div class="card-body">
              <div v-for="item in order.items" :key="item.product_id" class="product-list">
                <div class="d-flex">
                  <div class="left border d-flex rounded-2 overflow-hidden justify-content-center align-items-center wh-80"><img :src="imageOf(item.product_id)" :alt="item.title" class="img-fluid"></div>
                  <div class="right">
                    <div class="name"><a class="text-dark" @click="navigate('/products/' + item.product_id)">{{ item.title }}</a></div>
                    <div class="price">{{ formatPrice(item.unit_price) }} x {{ item.quantity }} = {{ formatPrice(item.unit_price * item.quantity) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card mb-lg-4 mb-2">
            <div class="card-header"><h6 class="card-title">订单总计</h6></div>
            <div class="card-body">
              <table class="table table-bordered border">
                <tbody>
                  <tr><td class="bg-light wp-200">商品总计</td><td><strong>{{ formatPrice(itemTotal) }}</strong></td><td class="bg-light wp-200">运费</td><td><strong>免运费</strong></td></tr>
                  <tr><td class="bg-light wp-200">实付金额</td><td><strong>{{ formatPrice(order.total) }}</strong></td><td class="bg-light wp-200">商品件数</td><td><strong>{{ order.items.length }}</strong></td></tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card mb-lg-4 mb-2">
            <div class="card-header"><h6 class="card-title">物流信息</h6></div>
            <div class="card-body">
              <table class="table"><thead><tr><th>配送状态</th><th>说明</th><th>更新时间</th></tr></thead>
                <tbody><tr><td>{{ shippingLabel[shipping] || shipping || '—' }}</td><td>物流状态来自订单物流接口，随订单实时变化</td><td>{{ order.created_at.replace('T', ' ').slice(0, 19) }}</td></tr></tbody>
              </table>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
