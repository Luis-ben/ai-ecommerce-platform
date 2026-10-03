<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { api } from '../api'
import { navigate } from '../router'
import { state } from '../store'

type Message = { role: 'user' | 'assistant'; content: string }
type Status = { enabled: boolean; mode: string; provider_configured: boolean; model: string; fallback: string }

const open = computed({ get: () => state.adminAiOpen, set: (value: boolean) => { state.adminAiOpen = value } })
const input = ref('')
const busy = ref(false)
const messages = ref<Message[]>([])
const status = ref<Status | null>(null)
const activeTab = ref<'home' | 'orders' | 'customers' | 'inventory' | 'settings'>('home')
const chatContainer = ref<HTMLElement | null>(null)

const shortcuts = [
  { key: 'orders', label: '订单处理', icon: 'bi bi-clipboard-check', prompt: '请汇总当前待处理订单、待发货订单与退款工单，提供处理建议' },
  { key: 'customers', label: '客户洞察', icon: 'bi bi-people', prompt: '请分析当前店铺客户规模、高价值客户消费排行与会员增长策略' },
  { key: 'inventory', label: '商品巡检', icon: 'bi bi-box-seam', prompt: '请全面巡检在售商品，找出库存低于10件的紧俏商品与滞销零销量款式' },
  { key: 'settings', label: '系统设置', icon: 'bi bi-gear', prompt: '请汇报当前 AI 模式、对接模型参数与安全防护配置' },
]

async function load(): Promise<void> {
  try { status.value = await api<Status>('/api/admin/ai/status') } catch { status.value = null }
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

async function send(text = input.value, tab?: typeof activeTab.value): Promise<void> {
  const message = text.trim()
  if (!message || busy.value) return
  if (tab) activeTab.value = tab
  messages.value.push({ role: 'user', content: message })
  input.value = ''
  busy.value = true
  await scrollToBottom()
  try {
    const result = await api<{ message: string }>('/api/admin/ai/chat', {
      method: 'POST',
      body: JSON.stringify({ message }),
    })
    messages.value.push({ role: 'assistant', content: result.message })
  } catch (error) {
    messages.value.push({ role: 'assistant', content: (error as Error).message })
  } finally {
    busy.value = false
    await scrollToBottom()
  }
}

function handleShortcut(key: string): void {
  if (key === 'settings') {
    open.value = false
    navigate('/admin/ai')
    return
  }
  const item = shortcuts.find(s => s.key === key)
  if (item) {
    void send(item.prompt, item.key as any)
  }
}

function newChat(): void {
  messages.value = []
  input.value = ''
  activeTab.value = 'home'
}

function goHome(): void {
  activeTab.value = 'home'
  if (!messages.value.length) {
    // 已经处于首页
  }
}

/** 智能解析消息中的链接并点击跳转 */
function onActionClick(url: string): void {
  open.value = false
  navigate(url)
}

/** 将 Markdown 中的链接 [文字](链接) 和粗体等渲染为友好节点 */
function parseMessageParts(content: string) {
  // 匹配 markdown link: [text](url)
  const regex = /\[([^\]]+)\]\(([^)]+)\)/g
  const parts: { type: 'text' | 'link'; text: string; url?: string }[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', text: content.slice(lastIndex, match.index) })
    }
    parts.push({ type: 'link', text: match[1], url: match[2] })
    lastIndex = regex.lastIndex
  }
  if (lastIndex < content.length) {
    parts.push({ type: 'text', text: content.slice(lastIndex) })
  }
  return parts
}

watch(open, (value) => { if (value) void load() })
</script>

<template>
  <button
    class="admin-ai-fab"
    type="button"
    :title="open ? '收起 AI 助手' : '打开 AI 助手'"
    @click="open = !open"
  >
    <span class="zeshop-ai-emoji" aria-hidden="true">💬</span>
    <span>AI</span>
  </button>

  <div v-if="open" class="admin-ai-layer">
    <div class="admin-ai-backdrop" @click="open = false"></div>
    <section class="admin-ai-drawer" @click.stop>
      <!-- 左侧边栏 (对齐截图) -->
      <aside class="admin-ai-nav">
        <div class="admin-ai-brand">
          <span>AI</span>
          <b>Beike AI</b>
        </div>
        <button
          class="admin-ai-nav-item"
          :class="{ active: activeTab === 'home' }"
          type="button"
          @click="goHome"
        >
          <i class="bi bi-house"></i>首页
        </button>
        <button class="admin-ai-nav-item" type="button" @click="newChat">
          <i class="bi bi-plus-lg"></i>新对话
        </button>

        <small class="admin-ai-nav-title">功能</small>
        <button
          class="admin-ai-nav-item"
          :class="{ active: activeTab === 'orders' }"
          type="button"
          @click="handleShortcut('orders')"
        >
          <i class="bi bi-grid"></i>订单处理
        </button>
        <button
          class="admin-ai-nav-item"
          :class="{ active: activeTab === 'customers' }"
          type="button"
          @click="handleShortcut('customers')"
        >
          <i class="bi bi-grid"></i>客户洞察
        </button>
        <button
          class="admin-ai-nav-item"
          :class="{ active: activeTab === 'inventory' }"
          type="button"
          @click="handleShortcut('inventory')"
        >
          <i class="bi bi-grid"></i>商品巡检
        </button>

        <small class="admin-ai-nav-title">系统</small>
        <button
          class="admin-ai-nav-item"
          type="button"
          @click="handleShortcut('settings')"
        >
          <i class="bi bi-gear"></i>设置
        </button>

        <div class="admin-ai-nav-status">
          <span :class="{ online: status?.enabled }"></span>
          {{ status?.provider_configured ? '外部模型已连接 (' + (status.model || 'qwen') + ')' : '本地专家模式' }}
        </div>
      </aside>

      <!-- 主区域 -->
      <main class="admin-ai-main">
        <header class="admin-ai-main-header">
          <button type="button" class="admin-ai-close" @click="open = false">×</button>
          <strong>{{ activeTab === 'home' ? '首页' : activeTab === 'orders' ? '订单处理 Agent' : activeTab === 'customers' ? '客户洞察 Agent' : activeTab === 'inventory' ? '商品巡检 Agent' : 'AI 助手' }}</strong>
          <span></span>
        </header>

        <!-- 首页欢迎卡片状态 (对齐截图) -->
        <div v-if="!messages.length" class="admin-ai-home">
          <span class="admin-ai-eyebrow">BEIKE AI</span>
          <h2>今天我能帮你<br><em>做什么？</em></h2>
          <p>订单处理、客户洞察、商品巡检，统一通过自然对话快速完成。</p>

          <form class="admin-ai-search" @submit.prevent="send()">
            <i class="bi bi-search"></i>
            <input v-model="input" placeholder="输入你想查询的店铺问题，例如：有哪些待发货订单…" />
            <kbd>⌘K</kbd>
          </form>

          <div class="admin-ai-shortcuts">
            <button
              v-for="item in shortcuts"
              :key="item.label"
              type="button"
              @click="handleShortcut(item.key)"
            >
              <i class="bi bi-stars"></i>
              <b>{{ item.label }}</b>
              <small>点击让 AI 帮你处理</small>
            </button>
          </div>
        </div>

        <!-- 对话流状态 -->
        <div v-else ref="chatContainer" class="admin-ai-chat">
          <div
            v-for="(item, index) in messages"
            :key="index"
            :class="['admin-ai-bubble', item.role]"
          >
            <b>{{ item.role === 'user' ? '我' : 'AI' }}</b>
            <div class="bubble-content">
              <template v-for="(part, pidx) in parseMessageParts(item.content)" :key="pidx">
                <button
                  v-if="part.type === 'link'"
                  type="button"
                  class="agent-action-btn"
                  @click="onActionClick(part.url!)"
                >
                  <i class="bi bi-arrow-right-circle me-1"></i>{{ part.text }}
                </button>
                <span v-else class="text-content">{{ part.text }}</span>
              </template>
            </div>
          </div>
          <div v-if="busy" class="text-secondary small d-flex align-items-center gap-2 ps-4">
            <span class="spinner-border spinner-border-sm text-primary"></span>
            <span>Beike AI 正在执行实时诊断与分析…</span>
          </div>
        </div>

        <!-- 底部输入框 -->
        <form class="admin-ai-composer" @submit.prevent="send()">
          <input v-model="input" placeholder="向 Beike AI 提问任何关于订单、客户或商品的事…" />
          <button type="submit" :disabled="busy || !input.trim()">
            <i class="bi bi-arrow-up"></i>
          </button>
        </form>
      </main>
    </section>
  </div>
</template>

<style scoped>
.bubble-content {
  max-width: 82%;
  padding: 12px 16px;
  background: #f7f9fb;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.7;
}
.admin-ai-bubble.user .bubble-content {
  background: #282d34;
  color: #fff;
}
.text-content {
  white-space: pre-line;
}
.agent-action-btn {
  display: inline-flex;
  align-items: center;
  margin: 6px 4px 6px 0;
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #0d6efd;
  color: #0d6efd;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 1px 3px rgba(13,110,253,0.1);
}
.agent-action-btn:hover {
  background: #0d6efd;
  color: #fff;
}
</style>
