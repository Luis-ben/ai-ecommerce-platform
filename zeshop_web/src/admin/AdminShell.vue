<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { navigate } from '../router'
import { logout, state } from '../store'
import { acquireAdminStyles, releaseAdminStyles } from './adminStyles'
import AdminAiOverlay from './AdminAiOverlay.vue'

const props = withDefaults(defineProps<{ active: string; title: string; activeChild?: string }>(), { activeChild: '' })

/** 模块与二级项对齐 BeikeShop 官方后台；每个一级入口都对应真实页面或明确的功能入口。 */
const modules = [
  { key: 'home', title: '首页', icon: 'bi bi-house', path: '/admin', children: [] as { label: string; path: string }[] },
  { key: 'orders', title: '订单', icon: 'bi bi-clipboard-check', path: '/admin/orders', children: [
    { label: '订单列表', path: '/admin/orders' },
    { label: '售后管理', path: '/admin/tickets' },
    { label: '聊天记录', path: '/admin/conversations' },
    { label: '售后原因', path: '/admin/orders/rma-reasons' },
    { label: '回收站', path: '/admin/orders/trash' },
  ] },
  { key: 'products', title: '商品', icon: 'bi bi-box-seam', path: '/admin/products', children: [
    { label: '商品管理', path: '/admin/products' },
    { label: '商品分类', path: '/admin/products/categories' },
    { label: '商品品牌', path: '/admin/products/brands' },
    { label: '属性组', path: '/admin/products/attribute-groups' },
    { label: '属性', path: '/admin/products/attributes' },
    { label: '高级筛选', path: '/admin/products/filters' },
    { label: '回收站', path: '/admin/products/trash' },
  ] },
  { key: 'customers', title: '客户', icon: 'bi bi-person-circle', path: '/admin/customers', children: [] },
  { key: 'articles', title: '文章', icon: 'bi bi-file-earmark-text', path: '/admin/articles', children: [] },
  { key: 'reports', title: '报表', icon: 'bi bi-bar-chart-line', path: '/admin/reports', children: [
    { label: '商品销售', path: '/admin/reports' },
    { label: '商品浏览', path: '/admin/reports/product-view' },
  ] },
  { key: 'ai', title: 'AI 助手', icon: 'bi bi-stars', path: '/admin/ai', children: [] },
  { key: 'ai-monitor', title: 'AI 监控', icon: 'bi bi-cpu', path: '/admin/ai-monitor', children: [] },
  { key: 'system', title: '系统', icon: 'bi bi-gear', path: '/admin/system', children: [] },
  { key: 'help', title: '帮助', icon: 'bi bi-question-circle', path: '/admin/help', children: [] },
]
const current = computed(() => modules.find((item) => item.key === props.active) || modules[0])
const currentChild = computed(() => props.activeChild || current.value.path)

async function doLogout(): Promise<void> { await logout(); navigate('/') }
function openModule(item: typeof modules[number]): void {
  if (item.key === 'ai') state.adminAiOpen = true
  navigate(item.path)
}
onMounted(acquireAdminStyles)
onUnmounted(releaseAdminStyles)
</script>

<!-- 结构对齐 BeikeShop 官方后台：深色顶栏 keep 白 / header-wrap + sidebar-box(左右两栏) + content-area -->
<template>
  <div class="admin-shell">
    <div class="header-content d-none d-lg-block">
      <div class="header-wrap">
        <div class="header-left">
          <div class="logo"><a @click="navigate('/admin')"><span class="zeshop-logo-mark">Z</span><span class="zeshop-logo-type"><strong>Zeshop</strong><small>SHOP SMARTER</small></span></a></div>
        </div>
        <div class="header-right">
          <div class="search-wrap">
            <div class="input-wrap">
              <div class="search-icon"><i class="bi bi-search"></i></div>
              <input type="text" class="form-control" placeholder="搜索后台功能">
              <button class="btn close-icon" type="button"><i class="bi bi-x-lg"></i></button>
            </div>
          </div>
          <ul class="navbar navbar-right">
            <li class="nav-item"><a class="nav-link" @click="navigate('/')"><i class="bi bi-shop fs-5"></i></a></li>
            <li class="nav-item me-3">
              <div class="dropdown">
                <a class="nav-link dropdown-toggle" @click="doLogout"><span class="ml-2">{{ state.user?.display_name || '未登录' }}</span></a>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <div class="header-mobile d-lg-none">
      <div class="header-mobile-wrap">
        <div class="header-mobile-left"><div class="mobile-open-menu" @click="navigate('/admin')"><i class="bi bi-list"></i></div></div>
        <div class="logo"><a @click="navigate('/admin')"><span class="zeshop-logo-mark">Z</span><span class="zeshop-logo-type"><strong>Zeshop</strong><small>SHOP SMARTER</small></span></a></div>
        <div class="header-mobile-right"><div class="mobile-to-front"><a @click="navigate('/')"><i class="bi bi-send"></i></a></div></div>
      </div>
    </div>

    <div class="main-content">
      <aside class="sidebar-box navbar-expand-xs border-radius-xl">
        <div class="sidebar-info">
          <div class="left">
            <ul class="list-unstyled navbar-nav">
              <li v-for="item in modules" :key="item.key" class="nav-item" :class="{ active: item.key === props.active }">
                <a class="nav-link" @click="openModule(item)"><i :class="item.icon"></i> <span>{{ item.title }}</span></a>
              </li>
            </ul>
          </div>
          <div v-if="current.children.length" class="right">
            <h4 class="title">{{ current.title }}</h4>
            <ul class="list-unstyled navbar-nav">
              <li v-for="child in current.children" :key="child.path" class="nav-item" :class="{ active: child.path === currentChild }">
                <a class="nav-link" @click="navigate(child.path)">{{ child.label }}</a>
              </li>
            </ul>
          </div>
        </div>
      </aside>

      <div id="content">
        <div class="content-area mx-auto">
          <div class="page-title-box d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center page-title-left">
              <div class="btn btn-sm me-2 cursor-auto"><i class="bi bi-list"></i></div>
              <h5 class="page-title d-flex align-items-center">{{ props.title }}</h5>
            </div>
            <div class="text-nowrap page-title-right"><span class="text-secondary small">Zeshop 后台</span></div>
          </div>
          <div class="container-fluid p-0">
            <div class="content-info"><slot /></div>
          </div>
        </div>
      </div>
      <AdminAiOverlay />
    </div>
  </div>
</template>
