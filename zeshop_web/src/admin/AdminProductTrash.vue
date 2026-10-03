<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify, type Product } from '../store'
const items = ref<Product[]>([]); const loading = ref(true)
async function load() { try { items.value = (await api<{ items: Product[] }>('/api/admin/products/trash')).items } catch (error) { notify((error as Error).message) } finally { loading.value = false } }
async function restore(id: string) { try { await api('/api/admin/products/' + id + '/restore', { method: 'POST' }); notify('商品已恢复'); await load() } catch (error) { notify((error as Error).message) } }
onMounted(load)
</script>
<template>
<AdminShell active="products" active-child="/admin/products/trash" title="商品回收站">
<p v-if="!isStaff" class="alert alert-warning">该页面需要商户角色。</p>
<template v-else><div class="card"><div class="card-body"><div v-if="loading">加载中…</div><table v-else-if="items.length" class="table"><tbody><tr v-for="item in items" :key="item.id"><td>{{ item.id }}</td><td>{{ item.title }}</td><td>{{ item.price }}</td><td><button class="btn btn-sm btn-outline-primary" type="button" @click="restore(item.id)">恢复</button></td></tr></tbody></table><div v-else class="text-center py-5 text-secondary">回收站为空</div></div></div></template>
</AdminShell>
</template>
