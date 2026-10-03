<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify } from '../store'

const props = defineProps<{ kind: string; title: string }>()

type Term = {
  id: string
  name: string
  kind?: string
  sort_order?: number
  active?: boolean
  created_at?: string
  logo?: string
  first_letter?: string
}

const items = ref<Term[]>([])
const name = ref('')
const keyword = ref('')
const showAdd = ref(false)
const editingItem = ref<Term | null>(null)
const editName = ref('')
const loading = ref(true)

// 根据 kind 决定属于哪个模块
const activeModule = computed(() => (props.kind === 'rma-reasons' ? 'orders' : 'products'))
const activeChildPath = computed(() => (props.kind === 'rma-reasons' ? '/admin/orders/rma-reasons' : '/admin/products/' + props.kind))

const filteredItems = computed(() => {
  if (!keyword.value.trim()) return items.value
  const kw = keyword.value.trim().toLowerCase()
  return items.value.filter(item => item.name.toLowerCase().includes(kw))
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<{ items: Term[] }>('/api/catalog/terms/' + props.kind)
    items.value = data.items
    // 如果是售后原因且没有数据，自动补充 BeikeShop 默认的 5 条原因
    if (props.kind === 'rma-reasons' && items.value.length === 0) {
      const defaults = ['未收到货', '发错商品', '错误下单', '商品损坏请添加备注', '其他请添加备注']
      for (const d of defaults) {
        await api('/api/catalog/terms/' + props.kind, { method: 'POST', body: JSON.stringify({ name: d }) }).catch(() => {})
      }
      const refreshed = await api<{ items: Term[] }>('/api/catalog/terms/' + props.kind)
      items.value = refreshed.items
    }
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function create(): Promise<void> {
  if (!name.value.trim()) {
    notify('请填写名称')
    return
  }
  try {
    await api('/api/catalog/terms/' + props.kind, {
      method: 'POST',
      body: JSON.stringify({ name: name.value.trim() }),
    })
    name.value = ''
    showAdd.value = false
    notify('已保存')
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

function startEdit(item: Term): void {
  editingItem.value = item
  editName.value = item.name
}

async function updateTerm(): Promise<void> {
  if (!editingItem.value) return
  if (!editName.value.trim()) {
    notify('请填写名称')
    return
  }
  try {
    await api('/api/catalog/terms/' + props.kind + '/' + encodeURIComponent(editingItem.value.id), {
      method: 'PUT',
      body: JSON.stringify({ name: editName.value.trim() }),
    })
    notify('已更新修改')
    editingItem.value = null
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

async function remove(id: string): Promise<void> {
  if (!confirm('确定删除该项？')) return
  try {
    await api('/api/catalog/terms/' + props.kind + '/' + encodeURIComponent(id), { method: 'DELETE' })
    notify('已删除')
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

onMounted(load)
</script>

<!-- 对齐 BeikeShop 售后原因 (img_3)、商品分类 (img_5)、品牌管理 (img_6)、属性组 (img_7) -->
<template>
  <AdminShell :active="activeModule" :active-child="activeChildPath" :title="props.title">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请以 merchant@example.com 登录。</p>
    <template v-else>
      <div class="card h-min-600">
        <div class="card-body">
          <!-- 顶部操作栏 -->
          <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
            <div class="d-flex gap-2 align-items-center">
              <button
                class="btn btn-warning text-white btn-sm px-3"
                style="background-color: #ff5722; border-color: #ff5722"
                type="button"
                @click="showAdd = !showAdd"
              >
                <i class="bi bi-plus-lg me-1"></i>{{ props.kind === 'rma-reasons' ? '添加' : ('创建' + props.title.replace('管理', '')) }}
              </button>
              <div v-if="props.kind !== 'rma-reasons'" class="input-group input-group-sm" style="max-width: 260px">
                <input v-model="keyword" class="form-control" placeholder="在此处输入您的搜索" />
                <button class="btn btn-outline-secondary" type="button" @click="keyword = ''">
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
            </div>
            <div class="text-secondary small">共 {{ filteredItems.length }} 条记录</div>
          </div>

          <!-- 新增表单卡片 -->
          <div v-if="showAdd" class="bg-light border rounded-3 p-3 mb-4">
            <h6 class="fw-bold mb-2">添加新项</h6>
            <div class="d-flex gap-2">
              <input
                v-model="name"
                class="form-control"
                placeholder="请输入名称"
                style="max-width: 320px"
                @keyup.enter="create"
              />
              <button class="btn btn-primary btn-sm px-3" type="button" @click="create">确认保存</button>
              <button class="btn btn-outline-secondary btn-sm" type="button" @click="showAdd = false">取消</button>
            </div>
          </div>

          <!-- 编辑表单卡片 -->
          <div v-if="editingItem" class="bg-warning-subtle border border-warning rounded-3 p-3 mb-4">
            <h6 class="fw-bold mb-2 text-warning-emphasis">编辑项：{{ editingItem.name }} (ID: {{ editingItem.id }})</h6>
            <div class="d-flex gap-2">
              <input
                v-model="editName"
                class="form-control"
                placeholder="请输入新名称"
                style="max-width: 320px"
                @keyup.enter="updateTerm"
              />
              <button class="btn btn-warning text-white btn-sm px-3" type="button" @click="updateTerm">保存修改</button>
              <button class="btn btn-outline-secondary btn-sm" type="button" @click="editingItem = null">取消</button>
            </div>
          </div>

          <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
          <div v-else-if="filteredItems.length" class="table-push">
            <!-- 售后原因样式 (img_3.png) -->
            <table v-if="props.kind === 'rma-reasons'" class="table table-hover align-middle">
              <thead>
                <tr>
                  <th style="width: 120px">ID</th>
                  <th>名称</th>
                  <th class="text-end pe-4" style="width: 180px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in filteredItems" :key="item.id">
                  <td class="text-secondary small">{{ idx + 1 }}</td>
                  <td class="fw-medium">{{ item.name }}</td>
                  <td class="text-end pe-4 text-nowrap">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="startEdit(item)">
                      编辑
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="remove(item.id)">
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- 商品品牌样式 (img_6.png) -->
            <table v-else-if="props.kind === 'brands'" class="table table-hover align-middle">
              <thead>
                <tr>
                  <th style="width: 80px">ID</th>
                  <th>品牌名称</th>
                  <th>图标</th>
                  <th>排序</th>
                  <th>首字母</th>
                  <th>状态</th>
                  <th class="text-end pe-4" style="width: 180px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in filteredItems" :key="item.id">
                  <td class="text-secondary small">{{ idx + 1 }}</td>
                  <td class="fw-bold">{{ item.name }}</td>
                  <td>
                    <div class="border rounded p-1 d-inline-flex align-items-center justify-content-center" style="width: 50px; height: 35px; background: #fff">
                      <span class="small fw-semibold text-muted">{{ item.name.slice(0, 2).toUpperCase() }}</span>
                    </div>
                  </td>
                  <td class="text-secondary">{{ item.sort_order ?? idx + 1 }}</td>
                  <td class="fw-semibold">{{ item.first_letter || item.name[0]?.toUpperCase() }}</td>
                  <td><span class="text-success fw-medium">启用</span></td>
                  <td class="text-end pe-4 text-nowrap">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="startEdit(item)">
                      编辑
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="remove(item.id)">
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- 属性组 / 属性 / 分类 / 筛选通用表格 (img_5, img_7) -->
            <table v-else class="table table-hover align-middle">
              <thead>
                <tr>
                  <th style="width: 100px">ID</th>
                  <th>名称</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th v-if="props.kind === 'attribute-groups'">创建时间</th>
                  <th class="text-end pe-4" style="width: 180px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in filteredItems" :key="item.id">
                  <td class="text-secondary small">{{ idx + 1 }}</td>
                  <td class="fw-medium">
                    <i v-if="props.kind === 'categories'" class="bi bi-caret-right-fill text-muted me-1 small"></i>
                    {{ item.name }}
                  </td>
                  <td class="text-secondary">{{ item.sort_order ?? 0 }}</td>
                  <td><span class="badge bg-success-subtle text-success border border-success-subtle">启用</span></td>
                  <td v-if="props.kind === 'attribute-groups'" class="text-secondary small">
                    {{ item.created_at || '2026-10-01 00:00:00' }}
                  </td>
                  <td class="text-end pe-4 text-nowrap">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="startEdit(item)">
                      编辑
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="remove(item.id)">
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-secondary">
            <i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">暂无数据</p>
          </div>
        </div>
      </div>
    </template>
  </AdminShell>
</template>
