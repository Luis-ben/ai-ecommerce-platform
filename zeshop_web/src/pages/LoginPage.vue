<script setup lang="ts">
import { ref } from 'vue'
import { navigate } from '../router'
import { login, notify, register, state } from '../store'

const email = ref('customer@example.com')
const password = ref('')
const displayName = ref('')
const registerMode = ref(false)
const accounts = [
  { email: 'customer@example.com', label: '顾客', role: '下单 / 查订单 / 申请售后' },
  { email: 'merchant@example.com', label: '商户', role: '商品管理 / 知识库' },
  { email: 'agent@example.com', label: '客服', role: '客服工作台 / 处理工单' },
]

async function submit(): Promise<void> {
  if (!email.value.trim()) { notify('请输入邮箱'); return }
  if (registerMode.value && !displayName.value.trim()) { notify('请输入姓名'); return }
  if (registerMode.value && password.value.length < 8) { notify('密码至少需要 8 位'); return }
  try {
    if (registerMode.value) {
      await register(email.value.trim(), password.value, displayName.value)
      navigate('/account')
    } else {
      await login(email.value.trim(), password.value)
      navigate(email.value.trim().toLowerCase() === 'customer@example.com' ? '/account' : '/admin')
    }
  } catch (error) {
    notify((error as Error).message)
  }
}

async function useDemo(value: string): Promise<void> {
  registerMode.value = false
  email.value = value
  password.value = ''
  await submit()
}
</script>

<!-- 结构对齐 BeikeShop 主题 account/login.blade.php：登录卡 + 右侧注册/演示账号卡 -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span class="text-dark">登录</span></nav></div>
  </div>
  <div class="container" id="page-login">
    <div class="hero-content pb-3 pb-lg-5 text-center"><h1 class="hero-heading">登录</h1></div>
    <div class="login-wrap">
      <div class="card">
        <form @submit.prevent="submit">
          <div class="login-item-header card-header"><h6 class="text-uppercase mb-0">登录</h6></div>
          <div class="card-body px-md-2">
            <div class="mb-3">
              <label class="form-label">邮箱</label>
              <input v-model="email" class="form-control" type="email" placeholder="邮箱地址" autocomplete="email">
            </div>
            <div v-if="registerMode" class="mb-3">
              <label class="form-label">姓名</label>
              <input v-model="displayName" class="form-control" type="text" placeholder="您的姓名" autocomplete="name">
            </div>
            <div class="mb-3">
              <label class="form-label">密码</label>
              <input v-model="password" class="form-control" type="password" :placeholder="registerMode ? '至少 8 位' : '演示账号可留空'" autocomplete="current-password">
            </div>
            <p class="text-muted small">{{ registerMode ? '真实账号使用服务端密码哈希保存。' : '演示环境可不填密码；生产环境必须使用密码登录。' }}</p>
            <div class="mt-4 mb-3">
              <button type="submit" class="btn btn-dark btn-lg w-100 fw-bold" :disabled="state.busy"><i class="bi" :class="registerMode ? 'bi-person-plus' : 'bi-box-arrow-in-right'"></i> {{ registerMode ? '注册并登录' : '登录' }}</button>
            </div>
            <button type="button" class="btn btn-link btn-sm text-secondary" @click="registerMode = !registerMode">{{ registerMode ? '已有账号？返回登录' : '注册真实账号' }}</button>
          </div>
        </form>
      </div>

      <div class="d-flex vr-wrap d-none d-md-flex"><div class="vr bg-secondary"></div></div>

      <div class="card">
        <div class="login-item-header card-header"><h6 class="text-uppercase mb-0">演示账号</h6></div>
        <div class="card-body px-md-2">
          <button v-for="account in accounts" :key="account.email" type="button" class="btn btn-outline-dark w-100 mb-2 text-start" @click="useDemo(account.email)">
            <i class="bi bi-person"></i> {{ account.label }} · {{ account.email }}
            <span class="d-block small text-muted">{{ account.role }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
