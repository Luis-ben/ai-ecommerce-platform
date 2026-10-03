<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AccountSidebar from '../components/AccountSidebar.vue'
import { navigate } from '../router'
import { createTicket, isLoggedIn, loadMyTickets, state } from '../store'

const intent = ref<'RETURN_REQUEST' | 'COMPLAINT'>('RETURN_REQUEST')
const statusLabel: Record<string, string> = { OPEN: '待处理', IN_PROGRESS: '处理中', CLOSED: '已关闭' }
const intentLabel: Record<string, string> = { RETURN_REQUEST: '退货', COMPLAINT: '投诉' }
onMounted(() => { void loadMyTickets() })
</script>

<!-- 结构对齐 BeikeShop 主题 account/rmas/index.blade.php -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span class="text-dark">售后服务</span></nav></div>
  </div>
  <div class="container">
    <div class="row">
      <AccountSidebar active="tickets" />
      <div class="col-12 col-md-9">
        <div v-if="!isLoggedIn" class="card account-card"><div class="card-body text-center py-5"><p class="text-muted">登录后查看售后工单</p><button class="btn btn-dark" @click="navigate('/login')">去登录</button></div></div>
        <div v-else class="card mb-4 account-card order-wrap h-min-600">
          <div class="card-header d-flex justify-content-between align-items-center"><h5 class="card-title">售后服务</h5></div>
          <div class="card-body">
            <div class="d-flex align-items-end gap-2 mb-3">
              <div><label class="form-label mb-1">工单类型</label><select v-model="intent" class="form-select"><option value="RETURN_REQUEST">退货申请</option><option value="COMPLAINT">投诉</option></select></div>
              <button class="btn btn-dark" :disabled="state.busy" @click="createTicket(intent)">提交工单</button>
              <button class="btn btn-outline-secondary" @click="loadMyTickets()">刷新</button>
            </div>
            <div class="table-responsive">
              <table class="table">
                <thead><tr><th>工单号</th><th class="text-nowrap">服务类型</th><th class="text-nowrap">状态</th><th class="text-nowrap">关联订单</th><th class="text-nowrap">创建时间</th></tr></thead>
                <tbody>
                  <tr v-for="ticket in state.tickets" :key="ticket.ticket_id">
                    <td><div class="text-ellipsis line-2 w-min-100 w-max-300">{{ ticket.ticket_id }}</div></td>
                    <td>{{ intentLabel[ticket.intent] || ticket.intent }}</td>
                    <td>{{ statusLabel[ticket.status] || ticket.status }}</td>
                    <td class="text-nowrap">{{ ticket.order_id || '—' }}</td>
                    <td class="text-nowrap">{{ ticket.created_at.replace('T', ' ').slice(0, 19) }}</td>
                  </tr>
                  <tr v-if="!state.tickets.length"><td colspan="5" class="border-0"><div class="d-flex flex-column align-items-center py-5 text-muted"><i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">还没有工单，可在订单详情点「申请退货」，或让 AI 客服帮你创建</p></div></td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
