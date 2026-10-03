<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'
import { navigate } from '../router'
import { isStaff, notify, state } from '../store'

type Ticket = { ticket_id: string; customer_id: string; order_id: string | null; intent: string; status: string; priority: string; created_at: string }

const tickets = ref<Ticket[]>([])
const filter = ref('')
const resolution = ref<Record<string, string>>({})
const reply = ref<Record<string, string>>({})
const statusText: Record<string, string> = { OPEN: '待处理', IN_PROGRESS: '处理中', CLOSED: '已关闭' }

async function load(): Promise<void> {
  try {
    tickets.value = (await api<{ items: Ticket[] }>('/api/support/tickets' + (filter.value ? '?status=' + filter.value : ''))).items
  } catch (error) {
    notify((error as Error).message)
  }
}

async function patch(ticket: Ticket, body: Record<string, string>): Promise<void> {
  try {
    await api('/api/support/tickets/' + ticket.ticket_id, { method: 'PATCH', body: JSON.stringify(body) })
    notify('工单已更新')
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

async function sendReply(ticket: Ticket): Promise<void> {
  const content = (reply.value[ticket.ticket_id] || '').trim()
  if (!content) return notify('请输入回复内容')
  try {
    await api('/api/support/tickets/' + ticket.ticket_id + '/messages', { method: 'POST', body: JSON.stringify({ content }) })
    reply.value[ticket.ticket_id] = ''
    notify('回复已发送')
  } catch (error) {
    notify((error as Error).message)
  }
}

onMounted(() => { void load() })
</script>

<template>
  <div class="container-fluid py-4">
    <nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span>客服工作台</span></nav>
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="fs-4 mb-0">客服工作台</h1>
      <div class="d-flex gap-2">
        <select v-model="filter" class="form-select" style="width:160px" @change="load()">
          <option value="">全部状态</option>
          <option value="OPEN">待处理</option>
          <option value="IN_PROGRESS">处理中</option>
          <option value="CLOSED">已关闭</option>
        </select>
        <button class="btn btn-outline-secondary" @click="load()">刷新</button>
      </div>
    </div>
    <p v-if="!isStaff" class="alert alert-warning">该页面需要客服或商户角色，请用 agent@example.com 登录。</p>
    <template v-else>
      <p v-if="!tickets.length" class="text-secondary text-center py-4">当前筛选下没有工单</p>
      <div v-for="ticket in tickets" :key="ticket.ticket_id" class="border p-3 mb-3">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <strong>{{ ticket.ticket_id }}</strong>
            <span class="small text-secondary ms-3">{{ ticket.intent === 'RETURN_REQUEST' ? '退货申请' : '投诉' }}</span>
            <span class="small text-secondary ms-3">顾客 {{ ticket.customer_id }}</span>
            <span class="small text-secondary ms-3">订单 {{ ticket.order_id || '—' }}</span>
          </div>
          <div class="d-flex align-items-center gap-2">
            <span class="badge bg-dark">{{ statusText[ticket.status] || ticket.status }}</span>
            <button v-if="ticket.status === 'OPEN'" class="btn btn-sm btn-outline-dark" @click="patch(ticket, { status: 'IN_PROGRESS' })">开始处理</button>
          </div>
        </div>
        <div v-if="ticket.status !== 'CLOSED'" class="row g-2 mt-3">
          <div class="col-12 col-md-6"><input v-model="resolution[ticket.ticket_id]" class="form-control" placeholder="处理结果（关闭工单必填）"></div>
          <div class="col-auto"><button class="btn btn-dark" :disabled="state.busy" @click="patch(ticket, { status: 'CLOSED', resolution: resolution[ticket.ticket_id] || '' })">关闭工单</button></div>
          <div class="col-12 col-md-6"><input v-model="reply[ticket.ticket_id]" class="form-control" placeholder="回复顾客"></div>
          <div class="col-auto"><button class="btn btn-outline-dark" @click="sendReply(ticket)">发送回复</button></div>
        </div>
      </div>
    </template>
  </div>
</template>
