<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify, type Order } from '../store'
const items = ref<Order[]>([]); const loading = ref(true)
async function load() { try { items.value = (await api<{ items: Order[] }>('/api/admin/orders?status=CLOSED')).items } catch (error) { notify((error as Error).message) } finally { loading.value = false } }
async function restore(id: string) { try { await api('/api/admin/orders/' + id, { method: 'PATCH', body: JSON.stringify({ status: 'PAID' }) }); notify('订单已恢复'); await load() } catch (error) { notify((error as Error).message) } }
onMounted(load)
</script>
<template>
<AdminShell active="orders" active-child="/admin/orders/trash" title="订单回收站">
<p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色。</p>
<template v-else><div class="card"><div class="card-body"><div v-if="loading">加载中…</div><table v-else-if="items.length" class="table table-hover"><thead><tr><th>订单号</th><th>客户</th><th>金额</th><th>操作</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td>{{ item.id }}</td><td>{{ item.id }}</td><td>{{ item.total }}</td><td><button class="btn btn-sm btn-outline-primary" type="button" @click="restore(item.id)">恢复为已付款</button></td></tr></tbody></table><div v-else class="text-center py-5 text-secondary">订单回收站为空</div></div></div></template>
</AdminShell>
</template>
