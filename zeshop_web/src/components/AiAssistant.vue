<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { API_BASE, readToken } from '../api'
import { navigate } from '../router'
import { addToCart, formatPrice, state } from '../store'

type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
  products?: { id?: string; product_id?: string; title: string; price: number; image?: string; stock?: number }[]
  confirmAction?: string // 'create_support_ticket'
  confirmIntent?: string
}

const chatOpen = computed({ get: () => state.chatOpen, set: (value: boolean) => { state.chatOpen = value } })
const chatInput = ref('')
const busy = ref(false)
const conversationId = ref('')
const messages = ref<ChatMessage[]>([{ role: 'assistant', content: '你好，我是 Zeshop AI 客服，可以帮你查商品、比价和发起售后。' }])
const body = ref<HTMLElement | null>(null)
const canSend = computed(() => chatInput.value.trim().length > 0 && !busy.value)

async function scrollToEnd(): Promise<void> {
  await nextTick()
  if (body.value) body.value.scrollTop = body.value.scrollHeight
}

/** 后端为 SSE 流式接口，按事件名增量渲染并收集商品卡片、确认动作。 */
async function ask(): Promise<void> {
  const question = chatInput.value.trim()
  if (!question || busy.value) return
  messages.value.push({ role: 'user', content: question })
  chatInput.value = ''
  busy.value = true
  const answer: ChatMessage = { role: 'assistant', content: '' }
  messages.value.push(answer)
  await scrollToEnd()
  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    const token = readToken()
    if (token) headers.Authorization = 'Bearer ' + token
    const response = await fetch(API_BASE + '/api/chat', {
      method: 'POST',
      headers,
      body: JSON.stringify({ message: question, conversation_id: conversationId.value || null }),
    })
    if (!response.ok || !response.body) throw new Error('客服服务暂不可用')
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const chunk = await reader.read()
      if (chunk.done) break
      buffer += decoder.decode(chunk.value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''
      for (const block of blocks) {
        const event = block.split('\n').find((line) => line.startsWith('event:'))?.slice(6).trim()
        const raw = block.split('\n').find((line) => line.startsWith('data:'))?.slice(5).trim()
        if (!raw) continue
        const data = JSON.parse(raw)
        if (event === 'text_delta') answer.content += data.content
        if (event === 'product_cards') answer.products = data.products
        if (event === 'message_start' && data.conversation_id) conversationId.value = data.conversation_id
        if (event === 'conversation' && data.conversation_id) conversationId.value = data.conversation_id
        if (event === 'confirmation_required') {
          answer.confirmAction = data.action
          answer.confirmIntent = data.intent
        }
      }
      await scrollToEnd()
    }
    if (!answer.content && !answer.products && !answer.confirmAction) {
      answer.content = '暂时没有查到相关信息，可以换个说法再问一次。'
    }
  } catch (error) {
    answer.content = (error as Error).message
  } finally {
    busy.value = false
    await scrollToEnd()
  }
}

/** 用户点击「确认创建工单」按钮后提交。 */
async function confirmTicket(intent: string): Promise<void> {
  if (!readToken()) {
    messages.value.push({ role: 'assistant', content: '请先登录，再创建售后工单。' })
    return
  }
  busy.value = true
  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + readToken() }
    const res = await fetch(API_BASE + '/api/support/tickets', {
      method: 'POST',
      headers,
      body: JSON.stringify({ intent, conversation_id: conversationId.value || null, confirmed: true }),
    })
    const data = await res.json()
    if (res.ok) {
      messages.value.push({ role: 'assistant', content: `售后工单已创建（${data.ticket_id}），客服将尽快联系您。` })
    } else {
      messages.value.push({ role: 'assistant', content: data?.error?.message || '工单创建失败，请稍后再试。' })
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '网络错误，工单创建失败。' })
  } finally {
    busy.value = false
    await scrollToEnd()
  }
}

function priceOf(item: { price: number }): string { return formatPrice(item.price) }
function productId(item: { id?: string; product_id?: string }): string { return item.id || item.product_id || '' }

/** 从 store 中查当前实时库存，兜底用商品数据中的值，避免硬编码。 */
function stockOf(item: { id?: string; product_id?: string; stock?: number }): number {
  const pid = productId(item)
  return state.products.find((p) => p.id === pid)?.stock ?? item.stock ?? 0
}

function goProduct(item: { id?: string; product_id?: string }): void {
  const pid = productId(item)
  if (pid) {
    navigate('/products/' + pid)
    chatOpen.value = false
  }
}

onMounted(() => { void scrollToEnd() })
</script>

<template>
  <div>
    <button class="zeshop-ai-fab" type="button" :title="chatOpen ? '收起 AI 客服' : 'AI 客服'" @click="chatOpen = !chatOpen">
      <span class="zeshop-ai-emoji" aria-hidden="true">💬</span><span>AI</span>
    </button>
    <div v-if="chatOpen" class="zeshop-ai-panel">
      <div class="zeshop-ai-header">
        <div><b>Zeshop AI 客服</b><small>在线 · 实时商品与政策查询</small></div>
        <button type="button" @click="chatOpen = false">×</button>
      </div>
      <div ref="body" class="zeshop-ai-body">
        <div v-for="(message, index) in messages" :key="index" :class="['zeshop-ai-message', message.role]">
          <div>
            <span v-if="message.content" style="white-space:pre-wrap">{{ message.content }}</span>
            <!-- 商品推荐卡片 -->
            <div v-if="message.products?.length" class="d-flex flex-column gap-2 mt-2">
              <div v-for="item in message.products" :key="productId(item)" class="zeshop-ai-product">
                <img :src="item.image || ''" :alt="item.title" style="cursor:pointer" @click="goProduct(item)">
                <div class="flex-grow-1">
                  <b style="cursor:pointer" @click="goProduct(item)">{{ item.title }}</b>
                  <strong>{{ priceOf(item) }}</strong>
                </div>
                <div class="d-flex flex-column gap-1">
                  <button
                    type="button" class="btn btn-sm btn-dark"
                    @click="addToCart({ id: productId(item), title: item.title, price: item.price, image: item.image, stock: stockOf(item) })"
                  >加购</button>
                  <button type="button" class="btn btn-sm btn-outline-secondary" @click="goProduct(item)">详情</button>
                </div>
              </div>
            </div>
            <!-- 工单确认按钮 -->
            <div v-if="message.confirmAction === 'create_support_ticket'" class="mt-2 d-flex gap-2">
              <button type="button" class="btn btn-sm btn-warning" @click="confirmTicket(message.confirmIntent || 'COMPLAINT')">
                ✅ 确认创建售后工单
              </button>
              <button type="button" class="btn btn-sm btn-outline-secondary" @click="messages.push({ role: 'assistant', content: '好的，如有需要随时告诉我。' })">
                取消
              </button>
            </div>
          </div>
        </div>
        <div v-if="busy" class="text-secondary small">正在查询商品库…</div>
      </div>
      <form class="zeshop-ai-input" @submit.prevent="ask">
        <input v-model="chatInput" placeholder="请输入您的问题，例如：推荐一款送礼的手袋">
        <button type="submit" :disabled="!canSend"><i class="bi bi-send"></i></button>
      </form>
    </div>
    <div v-if="state.toast" class="zeshop-toast">{{ state.toast }}</div>
    <div v-if="state.cart.length && chatOpen" class="zeshop-ai-cart-hint">购物车已有 {{ state.cart.length }} 种商品</div>
  </div>
</template>
