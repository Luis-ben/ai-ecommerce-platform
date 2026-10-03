<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AccountSidebar from '../components/AccountSidebar.vue'
import { navigate } from '../router'
import { formatPrice, imageOf, isLoggedIn, loadOrders, state } from '../store'

const active = ref('')
const statusLabel: Record<string, string> = { PAID: '已付款', PENDING: '待付款', CLOSED: '已关闭' }
const shippingLabel: Record<string, string> = { PROCESSING: '待发货', SHIPPED: '待收货', DELIVERED: '已完成' }
const tabs = [
  { value: '', label: '全部订单' },
  { value: 'PAID', label: '已付款' },
  { value: 'PROCESSING', label: '待发货' },
  { value: 'SHIPPED', label: '待收货' },
]
const filtered = computed(() => {
  if (!active.value) return state.orders
  if (active.value === 'PAID') return state.orders.filter((order) => order.status === 'PAID')
  return state.orders.filter((order) => order.shipping_status === active.value)
})
onMounted(() => { void loadOrders() })
</script>

<!-- 结构对齐 BeikeShop 主题 account/order.blade.php + shared/order_status -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span class="text-dark">我的订单</span></nav></div>
  </div>
  <div class="container">
    <div class="row">
      <AccountSidebar active="orders" />
      <div class="col-12 col-md-9">
        <div v-if="!isLoggedIn" class="card account-card"><div class="card-body text-center py-5"><p class="text-muted">登录后查看订单</p><button class="btn btn-dark" @click="navigate('/login')">去登录</button></div></div>
        <div v-else class="card mb-4 account-card order-wrap h-min-600">
          <div class="card-header d-flex justify-content-between align-items-center"><h5 class="card-title">我的订单</h5></div>
          <div class="card-body">
            <ul class="nav nav-tabs order-status-wrap">
              <li v-for="tab in tabs" :key="tab.value" class="nav-item" role="presentation">
                <a class="nav-link" :class="{ active: active === tab.value }" @click="active = tab.value">{{ tab.label }}</a>
              </li>
            </ul>
            <div class="table-responsive">
              <table class="table">
                <tbody v-for="order in filtered" :key="order.id">
                  <tr class="sep-row"><td colspan="4"></td></tr>
                  <tr class="head-tr"><td colspan="4"><span class="order-created me-4">{{ order.created_at.replace('T', ' ').slice(0, 19) }}</span><span class="order-number">订单号：{{ order.id }}</span></td></tr>
                  <tr v-for="(item, index) in order.items" :key="item.product_id" :class="{ 'first-tr': index === 0 }">
                    <td><div class="product-info"><div class="img border d-flex justify-content-center align-items-center wh-60"><img :src="imageOf(item.product_id)" :alt="item.title" class="img-fluid"></div><div class="name w-max-600"><a class="text-dark" @click="navigate('/products/' + item.product_id)">{{ item.title }}</a><div class="quantity mt-1 text-secondary">x {{ item.quantity }}</div></div></div></td>
                    <td v-if="index === 0" :rowspan="order.items.length">{{ formatPrice(order.total) }}</td>
                    <td v-if="index === 0" :rowspan="order.items.length"><span class="text-success">{{ statusLabel[order.status] || order.status }}</span></td>
                    <td v-if="index === 0" :rowspan="order.items.length" class="text-end"><a class="btn btn-outline-secondary btn-sm mb-2 w-100" @click="navigate('/orders/' + order.id)">查看订单</a></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!filtered.length" class="d-flex flex-column align-items-center py-5 text-muted"><i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">没有符合条件的订单</p></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
