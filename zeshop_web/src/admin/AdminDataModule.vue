<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify } from '../store'
const props = defineProps<{ path: string }>()
const items = ref<Record<string, unknown>[]>([]); const loading = ref(true)
const title = props.path.split('/').pop() || '后台数据'
async function load() { try { const result = await api<{ items?: Record<string, unknown>[] }>('/api/admin/data/' + encodeURIComponent(props.path)); items.value = result.items || [] } catch (error) { notify((error as Error).message) } finally { loading.value = false } }
onMounted(load)
</script>
<template>
<AdminShell :active="props.path.split('/')[0] || 'system'" :title="title">
<p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色。</p>
<template v-else><div class="card"><div class="card-body"><div class="mb-3">{{ title }}数据</div><div v-if="loading">加载中…</div><table v-else-if="items.length" class="table table-hover"><thead><tr><th v-for="key in Object.keys(items[0])" :key="key">{{ key }}</th></tr></thead><tbody><tr v-for="(item,index) in items" :key="index"><td v-for="key in Object.keys(items[0])" :key="key">{{ item[key] }}</td></tr></tbody></table><div v-else class="text-secondary">暂无数据</div></div></div></template>
</AdminShell>
</template>
