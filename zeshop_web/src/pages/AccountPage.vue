<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AccountSidebar from '../components/AccountSidebar.vue'
import { navigate } from '../router'
import { formatPrice, imageOf, loadMyTickets, loadOrders, state } from '../store'

const recent = computed(() => state.orders.slice(0, 5))
const shippingLabel: Record<string, string> = { PROCESSING: '待发货', SHIPPED: '待收货', DELIVERED: '已完成' }
onMounted(() => { void loadOrders(); void loadMyTickets() })
</script>

<!-- 结构对齐 BeikeShop 主题 account/account.blade.php -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span class="text-dark">个人中心</span></nav></div>
  </div>
  <div class="container">
    <div class="row">
      <AccountSidebar active="account" />
      <div class="col-12 col-md-9">
        <div class="card account-card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h5 class="card-title">我的订单</h5>
            <a class="text-muted fw-bold" @click="navigate('/orders')">全部订单</a>
          </div>
          <div class="card-body">
            <div class="d-flex flex-nowrap card-items mb-4 py-3">
              <a class="d-flex flex-column align-items-center" @click="navigate('/orders')"><i class="bi bi-wallet2"></i><span class="text-center">全部订单</span></a>
              <a class="d-flex flex-column align-items-center" @click="navigate('/orders')"><i class="bi bi-box-seam"></i><span class="text-center">待发货</span></a>
              <a class="d-flex flex-column align-items-center" @click="navigate('/orders')"><i class="bi bi-truck"></i><span class="text-center">待收货</span></a>
              <a class="d-flex flex-column align-items-center" @click="navigate('/tickets')"><i class="bi bi-arrow-counterclockwise"></i><span class="text-center">售后服务</span></a>
            </div>
            <div class="order-wrap rounded-2">
              <ul v-if="recent.length" class="list-unstyled orders-list table-responsive">
                <table class="table table-hover">
                  <tbody>
                    <tr v-for="order in recent" :key="order.id" class="align-middle">
                      <td style="width: 62px"><div class="img border wh-60 d-flex justify-content-center align-items-center rounded-1 overflow-hidden"><img :src="imageOf(order.items[0]?.product_id || '')" class="img-fluid" :alt="order.items[0]?.title"></div></td>
                      <td><div class="mb-2 text-nowrap">订单号：{{ order.id }}</div><div class="text-muted">下单时间：{{ order.created_at.replace('T', ' ').slice(0, 19) }}</div></td>
                      <td><span class="ms-4 text-nowrap d-inline-block">状态：<span class="text-success">{{ shippingLabel[order.shipping_status] || order.shipping_status }}</span></span></td>
                      <td><span class="ms-3 text-nowrap d-inline-block">金额：{{ formatPrice(order.total) }}</span></td>
                      <td><a class="btn btn-outline-secondary text-nowrap btn-sm" @click="navigate('/orders/' + order.id)">查看详情</a></td>
                    </tr>
                  </tbody>
                </table>
              </ul>
              <div v-else class="no-order d-flex flex-column align-items-center py-5">
                <div class="icon mb-2"><i class="bi bi-receipt fs-1"></i></div>
                <div class="text mb-3 text-muted">还没有订单，<a @click="navigate('/products')">去购买</a></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
