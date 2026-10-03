<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { API_BASE, api } from '../api'
import { isStaff, notify, state } from '../store'

type Stats = { store_id: string; products: { total: number; in_stock: number; out_of_stock: number }; orders: { total: number; revenue: number }; customers: number; tickets: { total: number; open: number }; stock_value: number }
type SettingsResponse = { store_id: string; settings: Record<string, string>; ai: { enabled: boolean; mode: string; provider_configured: boolean; model: string } }
const stats = ref<Stats | null>(null)
const settings = reactive({ store_name: '', customer_service_phone: '', checkout_free_shipping_threshold: '', ai_assistant_name: '', ai_mode: 'auto', ai_enabled: true })
const health = ref('')
const loading = ref(true)
const saving = ref(false)

async function load(): Promise<void> {
  try {
    stats.value = await api<Stats>('/api/admin/stats')
    const result = await api<SettingsResponse>('/api/admin/settings')
    Object.assign(settings, result.settings)
    settings.ai_enabled = result.ai.enabled
    settings.ai_mode = result.ai.mode
  } catch (error) { notify((error as Error).message) }
  try { const result = await fetch(API_BASE + '/api/health').then((response) => response.json()); health.value = result.status + ' · ' + result.backend } catch { health.value = '不可用' }
  finally { loading.value = false }
}
async function save(key: string, value: string | boolean): Promise<void> {
  saving.value = true
  try { await api('/api/admin/settings/' + encodeURIComponent(key), { method: 'PATCH', body: JSON.stringify({ value: String(value) }) }); notify('设置已保存') } catch (error) { notify((error as Error).message) } finally { saving.value = false }
}
async function saveAll(): Promise<void> { for (const [key, value] of Object.entries(settings)) await save(key, value) }
onMounted(load)
</script>

<template>
  <AdminShell active="system" title="系统">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请用 merchant@example.com 登录。</p>
    <template v-else>
      <div class="card mb-4"><div class="card-header"><h6 class="card-title mb-0">运行信息</h6></div><div class="card-body"><div class="row"><div class="col-lg-6"><table class="table table-borderless"><tbody><tr><td>接口地址：</td><td class="text-break">{{ API_BASE }}</td></tr><tr><td>健康检查：</td><td>{{ loading ? '检测中…' : health }}</td></tr><tr><td>登录账号：</td><td>{{ state.user?.display_name }}（{{ state.user?.role }}）</td></tr></tbody></table></div><div class="col-lg-6"><table class="table table-borderless"><tbody><tr><td>门店：</td><td>{{ stats?.store_id || '—' }}</td></tr><tr><td>商品：</td><td>{{ stats ? stats.products.total + ' 件（在售 ' + stats.products.in_stock + ' / 缺货 ' + stats.products.out_of_stock + '）' : '—' }}</td></tr><tr><td>库存金额：</td><td>{{ stats ? '$' + stats.stock_value.toFixed(2) : '—' }}</td></tr></tbody></table></div></div></div></div>
      <div class="card mb-4"><div class="card-header"><h6 class="card-title mb-0">基础设置</h6></div><div class="card-body"><div class="row g-3"><div class="col-md-6"><label class="form-label">店铺名称</label><input v-model="settings.store_name" class="form-control"></div><div class="col-md-6"><label class="form-label">客服热线</label><input v-model="settings.customer_service_phone" class="form-control"></div><div class="col-md-6"><label class="form-label">免运费门槛</label><input v-model="settings.checkout_free_shipping_threshold" class="form-control" type="number"></div></div></div></div>
      <div class="card mb-4"><div class="card-header"><h6 class="card-title mb-0">AI 设置</h6></div><div class="card-body"><div class="row g-3 align-items-end"><div class="col-md-4"><label class="form-label">助手名称</label><input v-model="settings.ai_assistant_name" class="form-control"></div><div class="col-md-4"><label class="form-label">运行模式</label><select v-model="settings.ai_mode" class="form-select"><option value="auto">自动：外部模型优先</option><option value="demo">演示：本地规则与数据库</option><option value="off">关闭</option></select></div><div class="col-md-4"><div class="form-check form-switch"><input id="ai-enabled" v-model="settings.ai_enabled" class="form-check-input" type="checkbox"><label class="form-check-label" for="ai-enabled">启用 AI 助手</label></div></div></div><p class="text-secondary small mt-3 mb-0">密钥不在页面回显；生产模型从服务端环境变量读取，演示模式不依赖外部模型。</p></div></div>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="saveAll">{{ saving ? '保存中…' : '保存全部设置' }}</button>
    </template>
  </AdminShell>
</template>
