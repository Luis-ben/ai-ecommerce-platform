import { computed, reactive } from 'vue'
import { api, readToken, writeToken } from './api'
import { demoProducts, homeBrands, homeCategories } from './data/catalog'

export type Product = {
  id: string
  title: string
  description?: string
  category?: string
  brand?: string
  image?: string | null
  price: number
  /** 本地种子目录带划线价，接口返回的商品可能没有该字段。 */
  originPrice?: number
  stock?: number
  stock_status?: string
  tag?: string | null
  sales?: number
  active?: boolean
}

export type User = { id: string; email: string; role: 'customer' | 'merchant' | 'agent'; store_id: string; display_name: string }

/** 游客购物车和服务端购物车统一成同一形状，页面只认这一种。 */
export type CartLine = { productId: string; title: string; image: string; quantity: number; unitPrice: number; subtotal: number }

export type Order = { id: string; status: string; total: number; shipping_status: string; created_at: string; items: { product_id: string; title: string; quantity: number; unit_price: number }[] }

export type Ticket = { ticket_id: string; customer_id: string; order_id: string | null; product_id: string | null; intent: string; status: string; priority: string; created_at: string }

const USER_KEY = 'zeshop-user'
const GUEST_CART_KEY = 'zeshop-guest-cart'
const FAVORITE_KEY = 'zeshop-favorites'

export const state = reactive({
  products: demoProducts as Product[],
  user: JSON.parse(localStorage.getItem(USER_KEY) || 'null') as User | null,
  cart: [] as CartLine[],
  orders: [] as Order[],
  tickets: [] as Ticket[],
  toast: '',
  busy: false,
  /** 顶栏「AI 客服」按钮与右下角浮层共用同一个开关。 */
  chatOpen: false,
  /** 后台侧栏与右下角 AI 浮窗共享一个打开状态。 */
  adminAiOpen: false,
  /** 收藏没有后端接口，按主题的 wishlist 交互存在本地。 */
  favorites: JSON.parse(localStorage.getItem('zeshop-favorites') || '[]') as string[],
})

export const brands = homeBrands
export const categories = homeCategories
export const isLoggedIn = computed(() => state.user !== null)
export const cartCount = computed(() => state.cart.reduce((sum, line) => sum + line.quantity, 0))
export const cartTotal = computed(() => state.cart.reduce((sum, line) => sum + line.subtotal, 0))
export const isStaff = computed(() => state.user?.role === 'agent' || state.user?.role === 'merchant')

export function isFavorite(productId: string): boolean {
  return state.favorites.includes(productId)
}

/** 主题的心形按钮走 bk.addWishlist，这里用本地收藏保持同样的即时反馈。 */
export function toggleFavorite(product: Product): void {
  const index = state.favorites.indexOf(product.id)
  if (index >= 0) {
    state.favorites.splice(index, 1)
    notify('已取消收藏')
  } else {
    state.favorites.push(product.id)
    notify('已加入收藏')
  }
  localStorage.setItem(FAVORITE_KEY, JSON.stringify(state.favorites))
}

export function formatPrice(value: number): string {
  return '$' + value.toFixed(2)
}

export function notify(message: string): void {
  state.toast = message
  window.setTimeout(() => { if (state.toast === message) state.toast = '' }, 2600)
}

export function imageOf(productId: string): string {
  return state.products.find((item) => item.id === productId)?.image || ''
}

function readGuestCart(): CartLine[] {
  return JSON.parse(localStorage.getItem(GUEST_CART_KEY) || '[]') as CartLine[]
}

function saveGuestCart(lines: CartLine[]): void {
  localStorage.setItem(GUEST_CART_KEY, JSON.stringify(lines))
}

export async function loadProducts(): Promise<void> {
  try {
    const data = await api<{ items: Product[] }>('/api/products')
    if (Array.isArray(data.items) && data.items.length) state.products = data.items
  } catch { /* 后端不可用时保留本地种子目录，页面仍可浏览 */ }
}

export async function loadProduct(id: string): Promise<Product | null> {
  try {
    return await api<Product>('/api/products/' + encodeURIComponent(id))
  } catch {
    return state.products.find((item) => item.id === id) || null
  }
}

type ServerCart = { items: { product_id: string; title: string; quantity: number; unit_price: number; subtotal: number }[] }

function toLines(payload: ServerCart): CartLine[] {
  return payload.items.map((item) => ({ productId: item.product_id, title: item.title, image: imageOf(item.product_id), quantity: item.quantity, unitPrice: item.unit_price, subtotal: item.subtotal }))
}

export async function refreshCart(): Promise<void> {
  if (!readToken()) {
    state.cart = readGuestCart()
    return
  }
  try {
    state.cart = toLines(await api<ServerCart>('/api/cart'))
  } catch (error) {
    notify((error as Error).message)
  }
}

export async function login(email: string, password = ''): Promise<void> {
  state.busy = true
  try {
    const data = await api<{ access_token: string; user: User }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email: email.trim().toLowerCase(), password: password || undefined }) })
    writeToken(data.access_token)
    state.user = data.user
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    await mergeGuestCart()
    await refreshCart()
    notify('已登录：' + data.user.display_name)
  } finally {
    state.busy = false
  }
}

export async function register(email: string, password: string, displayName: string): Promise<void> {
  state.busy = true
  try {
    const data = await api<{ access_token: string; user: User }>('/api/auth/register', { method: 'POST', body: JSON.stringify({ email: email.trim().toLowerCase(), password, display_name: displayName.trim() }) })
    writeToken(data.access_token)
    state.user = data.user
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    await mergeGuestCart()
    await refreshCart()
    notify('注册成功：' + data.user.display_name)
  } finally {
    state.busy = false
  }
}

export async function logout(): Promise<void> {
  writeToken('')
  state.user = null
  state.orders = []
  state.tickets = []
  localStorage.removeItem(USER_KEY)
  saveGuestCart([])
  state.cart = []
  notify('已退出登录')
}

/** 登录后把游客车合并进服务端购物车，避免登录前后加购结果不一致。 */
async function mergeGuestCart(): Promise<void> {
  const guest = readGuestCart()
  for (const line of guest) {
    try {
      await api('/api/cart/items', { method: 'POST', body: JSON.stringify({ product_id: line.productId, quantity: line.quantity }) })
    } catch { /* 库存变化导致合并失败时跳过该行，不阻断登录 */ }
  }
  saveGuestCart([])
}

export async function addToCart(product: Product, quantity = 1): Promise<boolean> {
  if (readToken()) {
    try {
      state.cart = toLines(await api<ServerCart>('/api/cart/items', { method: 'POST', body: JSON.stringify({ product_id: product.id, quantity }) }))
      notify('已加入购物车')
      return true
    } catch (error) {
      notify((error as Error).message)
      return false
    }
  }
  const lines = readGuestCart()
  const existing = lines.find((line) => line.productId === product.id)
  const stock = product.stock ?? 0
  if (existing) {
    if (existing.quantity + quantity > stock) { notify('库存不足'); return false }
    existing.quantity += quantity
    existing.subtotal = existing.quantity * existing.unitPrice
  } else {
    if (stock < quantity) { notify('库存不足'); return false }
    lines.push({ productId: product.id, title: product.title, image: product.image || '', quantity, unitPrice: product.price, subtotal: product.price * quantity })
  }
  saveGuestCart(lines)
  state.cart = lines
  notify('已加入购物车')
  return true
}

export async function setQuantity(productId: string, quantity: number): Promise<void> {
  if (quantity < 1) return removeLine(productId)
  if (readToken()) {
    try {
      state.cart = toLines(await api<ServerCart>('/api/cart/items/' + encodeURIComponent(productId), { method: 'PATCH', body: JSON.stringify({ product_id: productId, quantity }) }))
    } catch (error) {
      notify((error as Error).message)
    }
    return
  }
  const lines = readGuestCart()
  const line = lines.find((item) => item.productId === productId)
  if (!line) return
  line.quantity = quantity
  line.subtotal = quantity * line.unitPrice
  saveGuestCart(lines)
  state.cart = lines
}

export function removeLine(productId: string): void {
  state.cart = state.cart.filter((line) => line.productId !== productId)
  if (readToken()) {
    void api<ServerCart>('/api/cart/items/' + encodeURIComponent(productId), { method: 'DELETE' })
      .then((payload) => { state.cart = toLines(payload) })
      .catch((error) => notify((error as Error).message))
    return
  }
  saveGuestCart(state.cart)
}

export async function checkout(): Promise<Order | null> {
  if (!readToken()) {
    notify('请先登录后再结算')
    return null
  }
  state.busy = true
  try {
    const order = await api<Order>('/api/orders', { method: 'POST', body: JSON.stringify({ payment_method: 'SIMULATED' }) })
    await refreshCart()
    await loadOrders()
    notify('下单成功：' + order.id)
    return order
  } catch (error) {
    notify((error as Error).message)
    return null
  } finally {
    state.busy = false
  }
}

export async function loadOrders(): Promise<void> {
  if (!readToken()) { state.orders = []; return }
  try {
    state.orders = (await api<{ items: Order[] }>('/api/orders')).items
  } catch { /* 角色不符或未登录时保持空列表 */ }
}

export async function loadMyTickets(): Promise<void> {
  if (!readToken()) { state.tickets = []; return }
  try {
    state.tickets = (await api<{ items: Ticket[] }>('/api/support/my-tickets')).items
  } catch {
    state.tickets = []
  }
}

export async function createTicket(intent: 'COMPLAINT' | 'RETURN_REQUEST', orderId?: string): Promise<void> {
  state.busy = true
  try {
    const result = await api<{ ticket_id?: string; confirmation_required?: boolean }>('/api/support/tickets', { method: 'POST', body: JSON.stringify({ intent, order_id: orderId || null, confirmed: true }) })
    notify(result.confirmation_required ? '请确认后再提交售后工单' : '售后工单已创建')
    await loadMyTickets()
  } catch (error) {
    notify((error as Error).message)
  } finally {
    state.busy = false
  }
}
