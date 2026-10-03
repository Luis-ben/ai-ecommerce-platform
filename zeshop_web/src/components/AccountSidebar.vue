<script setup lang="ts">
import { computed } from 'vue'
import { navigate } from '../router'
import { logout, state } from '../store'

const props = defineProps<{ active: string }>()
const initial = computed(() => (state.user?.display_name || '游').slice(0, 1))

const menus = [
  { key: 'account', label: '个人中心', path: '/account' },
  { key: 'orders', label: '我的订单', path: '/orders' },
  { key: 'tickets', label: '售后服务', path: '/tickets' },
  { key: 'favorites', label: '我的收藏', path: '/favorites' },
]

async function doLogout(): Promise<void> {
  await logout()
  navigate('/')
}
</script>

<!-- 对应 BeikeShop 主题账户区的 <x-shop-sidebar /> -->
<template>
  <div class="col-12 col-md-3">
    <div class="card mb-4">
      <div class="card-body text-center">
        <div class="account-avatar mx-auto mb-2">{{ initial }}</div>
        <div class="fw-bold">{{ state.user?.display_name || '未登录' }}</div>
        <div class="text-muted small">{{ state.user?.email || '登录后可查看订单与售后' }}</div>
      </div>
      <ul class="list-group list-group-flush sidebar-widget">
        <li v-for="menu in menus" :key="menu.key" class="list-group-item" :class="{ active: props.active === menu.key }">
          <a class="d-block" @click="navigate(menu.path)">{{ menu.label }}</a>
        </li>
        <li class="list-group-item"><a class="d-block" @click="doLogout">退出登录</a></li>
      </ul>
    </div>
  </div>
</template>
