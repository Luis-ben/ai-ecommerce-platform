<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { navigate } from '../router'
import { notify, state } from '../store'

type TicketDetail = {
  ticket_id: string
  intent: string
  status: string
  priority: string
  resolution: string | null
  conversation_id: string | null
  customer_id?: string
  order_id?: string | null
}

type TicketMessage = { id: string; author_id: string; role: string; content: string; created_at: string }
type AiMessage = { role: string; content: string; source: string }

const props = defineProps<{ id: string }>()
const detail = ref<TicketDetail | null>(null)
const loading = ref(true)
const form = ref({ status: '', comment: '' })
const reply = ref('')
const ticketMessages = ref<TicketMessage[]>([])
const aiMessages = ref<AiMessage[]>([])
const chatHistoryLoading = ref(false)
const activeTab = ref<'status' | 'reply' | 'history'>('status')

const statusOptions = [
  { value: 'OPEN', label: '待处理' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'CLOSED', label: '已关闭' },
]
const statusLabel: Record<string, string> = { OPEN: '待处理', IN_PROGRESS: '处理中', CLOSED: '已关闭' }
const intentLabel: Record<string, string> = { RETURN_REQUEST: '退货申请', COMPLAINT: '投诉' }

async function load(): Promise<void> {
  loading.value = true
  try {
    detail.value = await api<TicketDetail>('/api/support/tickets/' + encodeURIComponent(props.id))
    form.value.status = detail.value.status
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
}

/** 加载工单关联的 AI 对话历史和客服回复记录。 */
async function loadChatHistory(): Promise<void> {
  chatHistoryLoading.value = true
  try {
    const data = await api<{ ai_messages: AiMessage[]; ticket_messages: TicketMessage[] }>(
      '/api/support/tickets/' + encodeURIComponent(props.id) + '/chat-history'
    )
    aiMessages.value = data.ai_messages || []
    ticketMessages.value = data.ticket_messages || []
  } catch {
    // 可能后端版本不支持，静默降级
    ticketMessages.value = []
    aiMessages.value = []
  } finally {
    chatHistoryLoading.value = false
  }
}

/** 状态变更对应 BeikeShop 的「修改状态 + 备注」提交，后端要求关闭时必须有结果。 */
async function updateStatus(): Promise<void> {
  if (!form.value.status) { notify('请选择状态'); return }
  const body: Record<string, string> = { status: form.value.status }
  if (form.value.status === 'CLOSED') {
    if (!form.value.comment.trim()) { notify('关闭工单必须填写处理结果'); return }
    body.resolution = form.value.comment.trim()
  }
  try {
    await api<{ status: string; resolution: string | null }>('/api/support/tickets/' + encodeURIComponent(props.id), { method: 'PATCH', body: JSON.stringify(body) })
    form.value.comment = ''
    await load()
    await loadChatHistory()
    notify('状态已更新')
  } catch (error) {
    notify((error as Error).message)
  }
}

async function sendReply(): Promise<void> {
  const content = reply.value.trim()
  if (!content) { notify('请输入回复内容'); return }
  try {
    await api('/api/support/tickets/' + encodeURIComponent(props.id) + '/messages', { method: 'POST', body: JSON.stringify({ content }) })
    reply.value = ''
    await loadChatHistory()
    notify('回复已发送')
  } catch (error) {
    notify((error as Error).message)
  }
}

onMounted(async () => {
  await load()
  await loadChatHistory()
})
</script>

<!-- 结构对齐 BeikeShop admin pages/rmas/info.blade.php -->
<template>
  <AdminShell active="orders" active-child="/admin/tickets" :title="'售后工单 ' + props.id">
    <div v-if="loading" class="card h-min-600"><div class="card-body text-center py-5 text-secondary">加载中…</div></div>
    <template v-else-if="detail">
      <!-- 工单基本信息 -->
      <div class="card mb-4">
        <div class="card-header d-flex align-items-center justify-content-between">
          <h6 class="card-title mb-0">工单详情</h6>
          <button class="btn btn-default btn-sm" type="button" @click="navigate('/admin/tickets')"><i class="bi bi-chevron-left"></i> 返回列表</button>
        </div>
        <div class="card-body">
          <div class="row">
            <div class="col-lg-4 col-12">
              <table class="table table-borderless">
                <tbody>
                  <tr><td>工单号：</td><td>{{ detail.ticket_id }}</td></tr>
                  <tr><td>服务类型：</td><td>{{ intentLabel[detail.intent] || detail.intent }}</td></tr>
                  <tr><td>当前状态：</td><td><span :class="detail.status === 'CLOSED' ? 'text-secondary' : detail.status === 'IN_PROGRESS' ? 'text-primary' : 'text-warning'">{{ statusLabel[detail.status] || detail.status }}</span></td></tr>
                  <tr><td>优先级：</td><td>{{ detail.priority }}</td></tr>
                </tbody>
              </table>
            </div>
            <div class="col-lg-4 col-12">
              <table class="table table-borderless">
                <tbody>
                  <tr><td>顾客 ID：</td><td>{{ detail.customer_id || '—' }}</td></tr>
                  <tr><td>关联订单：</td><td>{{ detail.order_id || '—' }}</td></tr>
                  <tr><td>会话 ID：</td><td class="text-break">{{ detail.conversation_id || '—' }}</td></tr>
                  <tr><td>处理结果：</td><td>{{ detail.resolution || '—' }}</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作区选项卡 -->
      <div class="card mb-4">
        <div class="card-header">
          <ul class="nav nav-tabs card-header-tabs">
            <li class="nav-item"><a class="nav-link" :class="{ active: activeTab === 'status' }" href="#" @click.prevent="activeTab = 'status'">更新状态</a></li>
            <li class="nav-item"><a class="nav-link" :class="{ active: activeTab === 'reply' }" href="#" @click.prevent="activeTab = 'reply'">回复顾客</a></li>
            <li class="nav-item"><a class="nav-link" :class="{ active: activeTab === 'history' }" href="#" @click.prevent="activeTab = 'history'; loadChatHistory()">聊天记录</a></li>
          </ul>
        </div>
        <div class="card-body">
          <!-- 状态更新 -->
          <div v-if="activeTab === 'status'">
            <div class="row g-3 align-items-center">
              <div class="col-12 col-md-3 text-secondary">当前状态：{{ statusLabel[detail.status] || detail.status }}</div>
              <div class="col-12 col-md-3">
                <select v-model="form.status" class="form-select form-select-sm">
                  <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </div>
              <div class="col-12 col-md-4">
                <textarea v-model="form.comment" class="form-control" rows="2" placeholder="备注（关闭工单时必填处理结果）"></textarea>
              </div>
              <div class="col-12 col-md-2">
                <button class="btn btn-primary btn-sm w-100" type="button" :disabled="state.busy" @click="updateStatus">更新状态</button>
              </div>
            </div>
          </div>

          <!-- 回复顾客 -->
          <div v-else-if="activeTab === 'reply'">
            <div class="row g-2">
              <div class="col-12 col-md-9"><textarea v-model="reply" class="form-control" rows="3" placeholder="输入给顾客的回复内容"></textarea></div>
              <div class="col-12 col-md-3 d-flex align-items-start"><button class="btn btn-dark btn-sm" type="button" @click="sendReply">发送回复</button></div>
            </div>
            <!-- 历史回复记录 -->
            <div v-if="ticketMessages.length" class="mt-3">
              <div class="text-secondary small mb-2">历史回复记录</div>
              <div v-for="msg in ticketMessages" :key="msg.id" class="border rounded p-2 mb-2 bg-light">
                <div class="d-flex justify-content-between">
                  <span class="badge" :class="msg.role === 'agent' ? 'bg-primary' : 'bg-secondary'">{{ msg.role === 'agent' ? '客服' : '系统' }}</span>
                  <small class="text-secondary">{{ msg.created_at.replace('T', ' ').slice(0, 19) }}</small>
                </div>
                <div class="mt-1">{{ msg.content }}</div>
              </div>
            </div>
          </div>

          <!-- AI 聊天记录 -->
          <div v-else-if="activeTab === 'history'">
            <div v-if="chatHistoryLoading" class="text-center py-3 text-secondary">加载中…</div>
            <div v-else-if="!aiMessages.length && !ticketMessages.length" class="text-center py-4 text-secondary">
              <i class="bi bi-chat-dots fs-2 mb-2 d-block"></i>
              <p class="mb-0">{{ detail.conversation_id ? '暂无消息记录' : '该工单未关联 AI 对话' }}</p>
            </div>
            <div v-else>
              <div v-if="aiMessages.length" class="mb-3">
                <div class="text-secondary small mb-2">AI 对话记录（来自 {{ detail.conversation_id }}）</div>
                <div v-for="(msg, i) in aiMessages" :key="i"
                  class="p-2 rounded mb-1"
                  :class="msg.role === 'user' ? 'bg-light text-end' : 'bg-primary bg-opacity-10'"
                  style="font-size:0.9rem"
                >
                  <span class="badge me-1" :class="msg.role === 'user' ? 'bg-secondary' : 'bg-primary'">{{ msg.role === 'user' ? '顾客' : 'AI' }}</span>
                  {{ msg.content }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <div v-else class="card h-min-600"><div class="card-body text-center py-5"><p class="text-secondary">工单不存在</p><button class="btn btn-dark" @click="navigate('/admin/tickets')">返回列表</button></div></div>
  </AdminShell>
</template>
