<script setup lang="ts">
import AdminShell from './AdminShell.vue'
import { navigate } from '../router'
import { isStaff } from '../store'

const entries = [
  { title: '商品管理', desc: '查看在售商品、行内改价与库存、创建新商品', path: '/admin/products' },
  { title: '订单列表', desc: '全店订单、状态筛选、展开查看订单明细', path: '/admin/orders' },
  { title: '售后管理', desc: '退货与投诉工单、受理、回复与关闭', path: '/admin/tickets' },
  { title: '客户', desc: '账号列表与角色筛选', path: '/admin/customers' },
  { title: '文章', desc: '知识库文档，写入后 AI 客服会引用', path: '/admin/articles' },
  { title: '报表', desc: '订单趋势、商品销量与客户消费排行', path: '/admin/reports' },
  { title: 'AI 助手', desc: '订单处理、客户洞察、商品库存巡检', path: '/admin/ai' },
  { title: '系统', desc: '接口地址、健康检查、门店与 AI 设置', path: '/admin/system' },
]
</script>

<!-- 对应 BeikeShop 官方后台 帮助 模块 -->
<template>
  <AdminShell active="help" title="帮助">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请用 merchant@example.com 登录。</p>
    <template v-else>
      <div class="card mb-4">
        <div class="card-header"><h6 class="card-title mb-0">后台功能导航</h6></div>
        <div class="card-body">
          <div class="row g-3">
            <div v-for="item in entries" :key="item.path" class="col-12 col-md-6">
              <div class="border rounded-3 p-3 h-100">
                <div class="fw-bold mb-1">{{ item.title }}</div>
                <div class="text-secondary small mb-2">{{ item.desc }}</div>
                <button class="btn btn-outline-primary btn-sm" type="button" @click="navigate(item.path)">进入</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h6 class="card-title mb-0">说明</h6></div>
        <div class="card-body text-secondary">
          <p class="mb-1">当前后台对应演示后端的商户／客服角色：商户可管理商品与知识库，客服可处理工单。</p>
          <p class="mb-0">返回前台请点右上角的店铺图标。</p>
        </div>
      </div>
    </template>
  </AdminShell>
</template>
