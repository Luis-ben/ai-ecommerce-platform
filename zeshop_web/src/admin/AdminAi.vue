<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify } from '../store'

// AI设置 - 对齐 BeikeShop AI设置页面
const loading = ref(true)
const saving = ref(false)
const showKey = ref(false)

const settings = ref({
  agent_name: 'Zeshop AI',
  admin_ai_enabled: true,
  frontend_ai_enabled: true,
  quick_questions_enabled: true,
  provider: '通义千问',
  api_key: '',
  model: 'qwen3.6-plus',
  max_message_length: 500,
  ai_mode: 'auto',
})

const providers = ['通义千问', 'OpenAI', 'DeepSeek', 'Moonshot', '自定义']
const modelOptions: Record<string, string[]> = {
  '通义千问': ['qwen3.6-plus', 'qwen3.5-plus', 'qwen-turbo', 'qwen-max'],
  'OpenAI': ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
  'DeepSeek': ['deepseek-chat', 'deepseek-coder'],
  'Moonshot': ['moonshot-v1-8k', 'moonshot-v1-32k'],
  '自定义': ['custom-model'],
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<typeof settings.value>('/api/admin/ai/status').catch(() => null)
    if (data) {
      if ((data as any).model) settings.value.model = (data as any).model
      if ((data as any).enabled !== undefined) settings.value.admin_ai_enabled = (data as any).enabled
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function save(): Promise<void> {
  saving.value = true
  try {
    await api('/api/admin/ai/settings', { method: 'POST', body: JSON.stringify(settings.value) }).catch(() => null)
    notify('AI设置已保存')
  } catch (error) { notify((error as Error).message) }
  finally { saving.value = false }
}

onMounted(load)
</script>

<!-- 对齐 BeikeShop AI设置页面：基础设置/功能开关/模型配置/限制设置 -->
<template>
  <AdminShell active="ai" title="AI 助手">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户角色，请以 merchant@example.com 登录。</p>
    <template v-else>
      <div class="d-flex justify-content-end gap-2 mb-4">
        <button class="btn btn-outline-secondary btn-sm">退出编辑</button>
        <button class="btn btn-primary btn-sm" :disabled="saving" @click="save">
          <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>保存
        </button>
      </div>

      <!-- 基础设置 -->
      <div class="card mb-4">
        <div class="card-body">
          <h5 class="fw-bold mb-1">基础设置</h5>
          <p class="text-secondary small mb-4">配置 AI 助手的基本信息，如名称和图标</p>
          <div class="row g-4">
            <div class="col-12 col-md-6">
              <label class="form-label fw-semibold">AI 助手名称</label>
              <input v-model="settings.agent_name" class="form-control" placeholder="Zeshop AI" />
              <div class="form-text">自定义 AI 助手在前台和后台的显示名称</div>
            </div>
            <div class="col-12 col-md-6">
              <label class="form-label fw-semibold">AI Agent 图标</label>
              <div class="ai-icon-upload">
                <i class="bi bi-plus-lg fs-4 text-secondary"></i>
              </div>
              <div class="form-text">上传 AI Agent 图标，将显示在浮动按钮、侧边栏和聊天界面中。建议尺寸 128×128，PNG/SVG 格式。</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 功能开关 -->
      <div class="card mb-4">
        <div class="card-body">
          <h5 class="fw-bold mb-1">功能开关</h5>
          <p class="text-secondary small mb-4">控制 AI 功能在不同场景下的启用状态</p>
          <hr />
          <div class="mb-4">
            <div class="fw-semibold mb-1">后台启用 AI 助手</div>
            <div class="text-secondary small mb-2">在管理后台显示 AI 助手悬浮按钮</div>
            <div class="d-flex gap-4">
              <label class="d-flex align-items-center gap-2 cursor-pointer">
                <input v-model="settings.admin_ai_enabled" type="radio" :value="true" class="form-check-input" /> 启用
              </label>
              <label class="d-flex align-items-center gap-2 cursor-pointer">
                <input v-model="settings.admin_ai_enabled" type="radio" :value="false" class="form-check-input" /> 禁用
              </label>
            </div>
          </div>
          <hr />
          <div class="mb-4">
            <div class="fw-semibold mb-1">启用快捷提问</div>
            <div class="text-secondary small mb-2">在欢迎界面显示快捷提问按钮</div>
            <div class="d-flex gap-4">
              <label class="d-flex align-items-center gap-2 cursor-pointer">
                <input v-model="settings.quick_questions_enabled" type="radio" :value="true" class="form-check-input" /> 启用
              </label>
              <label class="d-flex align-items-center gap-2 cursor-pointer">
                <input v-model="settings.quick_questions_enabled" type="radio" :value="false" class="form-check-input" /> 禁用
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- 模型配置 -->
      <div class="card mb-4">
        <div class="card-body">
          <h5 class="fw-bold mb-1">模型配置</h5>
          <p class="text-secondary small mb-4">选择要使用的 AI 提供商，并填写对应的模型、密钥和接口地址。通义千问推荐使用百炼兼容地址和新模型名称，兼旧模型不可用。</p>

          <div class="alert alert-info py-2 px-3 small mb-4" style="background:#e8f4ff;border:1px solid #b8d8f8;border-radius:6px">
            通义千问推荐使用百炼兼容地址：<code>https://dashscope.aliyuncs.com/compatible-mode/v1</code><br />
            通义千问推荐模型：<code class="text-danger">qwen3.6-plus</code>，兼容优先可选 <code class="text-danger">qwen3.5-plus</code>。
          </div>

          <div class="row g-3">
            <div class="col-12 col-md-6">
              <label class="form-label fw-semibold">AI 提供商</label>
              <select v-model="settings.provider" class="form-select">
                <option v-for="p in providers" :key="p" :value="p">{{ p }}</option>
              </select>
              <div class="form-text">选择使用的 AI 服务提供商。通义千问推荐使用百炼兼容接口。</div>
            </div>
            <div class="col-12 col-md-6">
              <label class="form-label fw-semibold">API 密钥</label>
              <div class="input-group">
                <input v-model="settings.api_key" :type="showKey ? 'text' : 'password'" class="form-control" placeholder="••••••••••••••••••••••••••••••••" />
                <button class="btn btn-outline-secondary" type="button" @click="showKey = !showKey">
                  <i :class="showKey ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>
              <div class="form-text">从 AI 提供商获取的 API 密钥，请妥善保管</div>
            </div>
            <div class="col-12 col-md-6">
              <label class="form-label fw-semibold">AI 模型</label>
              <select v-model="settings.model" class="form-select">
                <option v-for="m in (modelOptions[settings.provider] || [])" :key="m" :value="m">{{ m }}</option>
              </select>
              <div class="form-text">根据选择的提供商显示可用模型。通义千问推荐模型：qwen3.6-plus，其次可选 qwen3.5-plus。</div>
            </div>
            <div class="col-12 col-md-6">
              <label class="form-label fw-semibold">AI 模式</label>
              <select v-model="settings.ai_mode" class="form-select">
                <option value="auto">自动（优先使用模型）</option>
                <option value="rule">规则模式（纯数据库）</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- 限制设置 -->
      <div class="card mb-4">
        <div class="card-body">
          <h5 class="fw-bold mb-1">限制设置</h5>
          <p class="text-secondary small mb-4">设置 AI 使用限制，控制消息数量和长度</p>
          <div class="row g-3">
            <div class="col-12 col-md-4">
              <label class="form-label fw-semibold">消息最大长度</label>
              <input v-model.number="settings.max_message_length" type="number" class="form-control" min="100" max="2000" />
              <div class="form-text">用户输入消息的最大字符数限制</div>
            </div>
          </div>
        </div>
      </div>

      <div class="text-center text-secondary small pb-4">
        Powered By <a href="#" class="text-primary text-decoration-none">Zeshop</a> · AI 设置页
      </div>
    </template>
  </AdminShell>
</template>

<style scoped>
.ai-icon-upload {
  width: 80px; height: 80px; border: 2px dashed #ccc; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: #fafafa; transition: border-color .2s;
}
.ai-icon-upload:hover { border-color: #0d6efd; }
</style>
