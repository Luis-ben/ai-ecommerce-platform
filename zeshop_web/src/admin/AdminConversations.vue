<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { notify } from '../store'

type ConvItem = {
  conversation_id: string
  customer_id: string | null
  created_at: string
  last_message: string | null
  last_role: string | null
}
type ConvDetail = {
  conversation_id: string
  customer_id: string | null
  created_at: string
  messages: { id: string; role: string; content: string }[]
}

const conversations = ref<ConvItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(true)
const selected = ref<ConvDetail | null>(null)
const detailLoading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<{ items: ConvItem[]; total: number }>(`/api/admin/conversations?page=${page.value}&page_size=${pageSize}`)
    conversations.value = data.items
    total.value = data.total
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function viewDetail(conv: ConvItem): Promise<void> {
  selected.value = null
  detailLoading.value = true
  try {
    selected.value = await api<ConvDetail>(`/api/admin/conversations/${encodeURIComponent(conv.conversation_id)}/messages`)
  } catch (error) {
    notify((error as Error).message)
  } finally {
    detailLoading.value = false
  }
}

function prevPage(): void { if (page.value > 1) { page.value--; void load() } }
function nextPage(): void { if (page.value * pageSize < total.value) { page.value++; void load() } }

onMounted(load)
</script>

<template>
  <AdminShell active="orders" active-child="/admin/conversations" title="聊天记录">
    <div class="row g-4">
      <!-- 对话列表 -->
      <div class="col-12 col-lg-5">
        <div class="card h-min-600">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h6 class="card-title mb-0">对话列表（共 {{ total }} 条）</h6>
            <button class="btn btn-outline-secondary btn-sm" @click="load">刷新</button>
          </div>
          <div class="card-body p-0">
            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <div v-else-if="!conversations.length" class="text-center py-5 text-secondary">
              <i class="bi bi-chat-dots fs-1 mb-2 d-block"></i>
              <p class="mb-0">暂无对话记录</p>
            </div>
            <ul v-else class="list-group list-group-flush">
              <li
                v-for="conv in conversations"
                :key="conv.conversation_id"
                class="list-group-item list-group-item-action cursor-pointer py-3"
                :class="{ active: selected?.conversation_id === conv.conversation_id }"
                @click="viewDetail(conv)"
              >
                <div class="d-flex justify-content-between align-items-start">
                  <div class="flex-grow-1 me-2">
                    <div class="small text-truncate fw-medium">{{ conv.customer_id || '游客' }}</div>
                    <div class="small text-truncate mt-1" :class="selected?.conversation_id === conv.conversation_id ? 'text-white-50' : 'text-secondary'">
                      {{ conv.last_message || '（无消息）' }}
                    </div>
                  </div>
                  <div class="text-nowrap small" :class="selected?.conversation_id === conv.conversation_id ? 'text-white-50' : 'text-secondary'">
                    {{ conv.created_at.replace('T', ' ').slice(0, 16) }}
                  </div>
                </div>
              </li>
            </ul>
          </div>
          <div v-if="total > pageSize" class="card-footer d-flex justify-content-between align-items-center">
            <button class="btn btn-sm btn-outline-secondary" :disabled="page <= 1" @click="prevPage">上一页</button>
            <span class="text-secondary small">第 {{ page }} 页</span>
            <button class="btn btn-sm btn-outline-secondary" :disabled="page * pageSize >= total" @click="nextPage">下一页</button>
          </div>
        </div>
      </div>

      <!-- 对话详情 -->
      <div class="col-12 col-lg-7">
        <div class="card h-min-600">
          <div v-if="!selected && !detailLoading" class="card-body d-flex align-items-center justify-content-center text-secondary h-min-600">
            <div class="text-center">
              <i class="bi bi-chat-square-text fs-1 mb-3 d-block"></i>
              <p class="mb-0">点击左侧对话查看详情</p>
            </div>
          </div>
          <div v-else-if="detailLoading" class="card-body text-center py-5 text-secondary h-min-600">加载中…</div>
          <template v-else-if="selected">
            <div class="card-header">
              <div class="d-flex justify-content-between align-items-center">
                <h6 class="card-title mb-0">对话详情</h6>
                <button class="btn-close btn-sm" @click="selected = null"></button>
              </div>
              <div class="small text-secondary mt-1">
                顾客：{{ selected.customer_id || '游客' }} ·
                时间：{{ selected.created_at.replace('T', ' ').slice(0, 19) }}
              </div>
            </div>
            <div class="card-body overflow-auto" style="max-height:600px">
              <div v-if="!selected.messages.length" class="text-center py-4 text-secondary">暂无消息</div>
              <div v-for="(msg, i) in selected.messages" :key="i" class="mb-3">
                <div class="d-flex" :class="msg.role === 'user' ? 'justify-content-end' : ''">
                  <div
                    class="p-2 rounded-3"
                    :class="msg.role === 'user' ? 'bg-dark text-white' : 'bg-light'"
                    style="max-width:85%;font-size:0.9rem;white-space:pre-wrap;word-break:break-word"
                  >
                    <div class="small fw-bold mb-1" :class="msg.role === 'user' ? 'text-white-50' : 'text-secondary'">
                      {{ msg.role === 'user' ? '顾客' : 'AI 客服' }}
                    </div>
                    {{ msg.content }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </AdminShell>
</template>
