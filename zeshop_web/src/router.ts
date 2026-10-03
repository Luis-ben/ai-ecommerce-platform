import { computed, ref } from 'vue'

/** 单页应用的路由状态：地址栏与内存同步，页面组件只依赖 page / navigate。 */
export const route = ref(window.location.pathname + window.location.search)

export const query = computed(() => new URLSearchParams(route.value.split('?')[1] || ''))

export const page = computed(() => {
  const path = route.value.split('?')[0]
  const parts = path.split('/').filter(Boolean)
  if (parts.length === 0) return { name: 'home', param: '' }
  if (parts[0] === 'products') return parts[1] ? { name: 'product', param: decodeURIComponent(parts[1]) } : { name: 'catalog', param: '' }
  if (parts[0] === 'orders') return parts[1] ? { name: 'order-detail', param: decodeURIComponent(parts[1]) } : { name: 'orders', param: '' }
  if (parts[0] === 'checkout') return { name: 'checkout', param: '' }
  if (parts[0] === 'favorites') return { name: 'favorites', param: '' }
  if (parts[0] === 'admin') {
    const rest = parts.slice(1)
    if (!rest.length) return { name: 'admin-dashboard', param: '' }
    if (rest[0] === 'tickets') {
      return rest[1] ? { name: 'admin-ticket-detail', param: decodeURIComponent(rest[1]) } : { name: 'admin-tickets', param: '' }
    }
    if (rest[0] === 'ai-monitor') return { name: 'admin-ai-monitor', param: '' }
    if (rest[0] === 'orders') {
      if (rest[1] === 'rma-reasons') return { name: 'admin-catalog-terms', param: 'rma-reasons' }
      if (rest[1] === 'trash') return { name: 'admin-order-trash', param: '' }
      return { name: 'admin-orders', param: rest[1] || '' }
    }
    if (rest[0] === 'products') {
      if (['categories', 'brands', 'attribute-groups', 'attributes', 'filters', 'rma-reasons'].includes(rest[1])) {
        return { name: 'admin-catalog-terms', param: rest[1] }
      }
      if (rest[1] === 'trash') return { name: 'admin-product-trash', param: '' }
      return { name: 'admin-products', param: rest[1] || '' }
    }
    if (rest[0] === 'customers') {
      return { name: 'admin-customers', param: rest[1] || '' }
    }
    if (rest[0] === 'articles') {
      return { name: 'admin-articles', param: rest[1] || '' }
    }
    if (rest[0] === 'reports') {
      return { name: 'admin-reports', param: rest[1] === 'product-view' ? 'views' : (rest[1] || 'sales') }
    }
    if (['ai', 'system', 'help', 'conversations'].includes(rest[0])) {
      return { name: 'admin-' + rest[0], param: rest[1] || '' }
    }
    return { name: 'admin-dashboard', param: '' }
  }
  if (parts[0] === 'merchant' && parts[1] === 'products') return { name: 'admin-products', param: '' }
  if (parts[0] === 'console') return { name: 'admin-tickets', param: '' }
  return { name: parts[0], param: '' }
})

export function navigate(path: string): void {
  window.history.pushState({}, '', path)
  route.value = window.location.pathname + window.location.search
  // 用即时滚动代替 smooth：平滑滚动在页面切换时会表现为一次抖动/闪动
  window.scrollTo(0, 0)
}

window.addEventListener('popstate', () => {
  route.value = window.location.pathname + window.location.search
})
