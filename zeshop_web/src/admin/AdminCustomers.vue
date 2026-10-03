<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify } from '../store'

type AdminUser = {
  id: string; email: string; role: string; display_name: string
  store_id: string; customer_group?: string; active?: boolean; created_at?: string
}
type CustomerGroup = { id: string; code: string; name: string; discount_rate: number; min_spend: number }

const users = ref<AdminUser[]>([])
const groups = ref<CustomerGroup[]>([])
const loading = ref(true)
const subPage = ref<'list' | 'groups'>('list')

// 筛选（对齐BeikeShop：姓名/Email/客户组/状态）
const filterName = ref('')
const filterEmail = ref('')
const filterGroup = ref('')
const filterStatus = ref('')

// 客户组新建
const newGroupName = ref('')
const newGroupDesc = ref('')

const filtered = computed(() => users.value.filter((u) => {
  if (u.role !== 'customer') return false
  if (filterName.value && !u.display_name.toLowerCase().includes(filterName.value.toLowerCase())) return false
  if (filterEmail.value && !u.email.toLowerCase().includes(filterEmail.value.toLowerCase())) return false
  if (filterGroup.value && u.customer_group !== filterGroup.value) return false
  return true
}))

async function load(): Promise<void> {
  loading.value = true
  try {
    const [ud, gd] = await Promise.all([
      api<{ items: AdminUser[] }>('/api/admin/customers'),
      api<{ items: CustomerGroup[] }>('/api/admin/customer-groups'),
    ])
    users.value = ud.items
    groups.value = gd.items
  } catch (error) { notify((error as Error).message) }
  finally { loading.value = false }
}

async function toggleActive(user: AdminUser): Promise<void> {
  try {
    await api('/api/admin/customers/' + user.id, { method: 'PATCH', body: JSON.stringify({ active: !user.active }) })
    user.active = !user.active
  } catch (error) { notify((error as Error).message) }
}

async function deleteUser(id: string): Promise<void> {
  if (!confirm('确定删除该客户？')) return
  try { await api('/api/admin/customers/' + id, { method: 'DELETE' }); notify('已删除'); await load() }
  catch (error) { notify((error as Error).message) }
}

async function loginAs(user: AdminUser): Promise<void> {
  notify('代登录功能：' + user.email + '（演示门店不支持实际切换）')
}

async function createGroup(): Promise<void> {
  if (!newGroupName.value.trim()) { notify('请填写客户组名称'); return }
  try {
    await api('/api/admin/customer-groups', { method: 'POST', body: JSON.stringify({ name: newGroupName.value.trim(), code: newGroupName.value.trim(), description: newGroupDesc.value }) })
    notify('客户组已创建'); newGroupName.value = ''; newGroupDesc.value = ''; await load()
  } catch (error) { notify((error as Error).message) }
}

async function deleteGroup(id: string): Promise<void> {
  if (!confirm('确定删除？')) return
  try { await api('/api/admin/customer-groups/' + id, { method: 'DELETE' }); notify('已删除'); await load() }
  catch (error) { notify((error as Error).message) }
}

function resetFilter(): void { filterName.value = ''; filterEmail.value = ''; filterGroup.value = ''; filterStatus.value = '' }

onMounted(load)
</script>

<template>
  <AdminShell active="customers" :title="subPage === 'groups' ? '客户组' : '客户管理'">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请以 merchant@example.com 登录。</p>
    <template v-else>

      <!-- 子页面切换 -->
      <div class="d-flex gap-2 mb-4">
        <button class="btn btn-sm" :class="subPage === 'list' ? 'btn-primary' : 'btn-outline-secondary'" @click="subPage = 'list'">客户管理</button>
        <button class="btn btn-sm" :class="subPage === 'groups' ? 'btn-primary' : 'btn-outline-secondary'" @click="subPage = 'groups'">客户组</button>
      </div>

      <!-- ── 客户列表 ── -->
      <template v-if="subPage === 'list'">
        <div class="card h-min-600">
          <div class="card-body">
            <!-- 筛选栏 -->
            <div class="bg-light rounded-3 p-4 mb-4">
              <div class="row g-3 align-items-end">
                <div class="col-12 col-md-3">
                  <label class="filter-title">姓名</label>
                  <input v-model="filterName" class="form-control" placeholder="姓名" />
                </div>
                <div class="col-12 col-md-3">
                  <label class="filter-title">Email</label>
                  <input v-model="filterEmail" class="form-control" placeholder="Email" />
                </div>
                <div class="col-12 col-md-2">
                  <label class="filter-title">客户组</label>
                  <select v-model="filterGroup" class="form-select">
                    <option value="">请选择</option>
                    <option v-for="g in groups" :key="g.code" :value="g.code">{{ g.name }}</option>
                  </select>
                </div>
                <div class="col-12 col-md-2">
                  <label class="filter-title">状态</label>
                  <select v-model="filterStatus" class="form-select">
                    <option value="">全部</option>
                    <option value="active">启用</option>
                    <option value="disabled">禁用</option>
                  </select>
                </div>
                <div class="col-auto">
                  <button type="button" class="btn btn-primary btn-sm me-2" @click="load">筛选</button>
                  <button type="button" class="btn btn-outline-secondary btn-sm" @click="resetFilter">重置</button>
                </div>
              </div>
            </div>

            <div class="mb-3">
              <button class="btn btn-primary btn-sm">创建客户</button>
            </div>

            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <div v-else-if="filtered.length" class="table-push">
              <table class="table table-hover align-middle">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Email</th>
                    <th>姓名</th>
                    <th>注册来源</th>
                    <th>客户组</th>
                    <th>状态</th>
                    <th>审核</th>
                    <th>创建时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="user in filtered" :key="user.id">
                    <td class="text-secondary small">{{ filtered.indexOf(user) + 1 }}</td>
                    <td>{{ user.email }}</td>
                    <td>{{ user.display_name }}</td>
                    <td><span class="text-secondary small">pc</span></td>
                    <td>{{ groups.find(g => g.code === user.customer_group)?.name || '白银' }}</td>
                    <td>
                      <!-- 开关 toggle -->
                      <div class="form-check form-switch mb-0">
                        <input class="form-check-input" type="checkbox" :checked="user.active !== false" @change="toggleActive(user)" />
                      </div>
                    </td>
                    <td>
                      <select class="form-select form-select-sm" style="width:auto;min-width:90px">
                        <option>已审核</option>
                        <option>待审核</option>
                      </select>
                    </td>
                    <td class="text-nowrap small">{{ (user.created_at || '').replace('T', ' ').slice(0, 19) || '-' }}</td>
                    <td class="text-nowrap">
                      <button class="btn btn-outline-secondary btn-sm me-1" @click="loginAs(user)">登录</button>
                      <button class="btn btn-outline-danger btn-sm" @click="deleteUser(user.id)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-secondary">
              <i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">没有符合条件的客户</p>
            </div>
          </div>
        </div>
      </template>

      <!-- ── 客户组 ── -->
      <template v-else>
        <div class="card h-min-600">
          <div class="card-body">
            <!-- 新建 -->
            <div class="d-flex gap-2 mb-4 align-items-end">
              <div>
                <label class="filter-title">名称</label>
                <input v-model="newGroupName" class="form-control" placeholder="客户组名称" style="width:200px" />
              </div>
              <div>
                <label class="filter-title">描述</label>
                <input v-model="newGroupDesc" class="form-control" placeholder="描述" style="width:200px" />
              </div>
              <button class="btn btn-primary btn-sm" @click="createGroup">创建客户组</button>
            </div>
            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <div v-else class="table-push">
              <table class="table table-hover align-middle">
                <thead><tr><th>ID</th><th>名称</th><th>描述</th><th>创建时间</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="(g, i) in groups" :key="g.id">
                    <td>{{ i + 1 }}</td>
                    <td>{{ g.name }}</td>
                    <td class="text-secondary">{{ g.name + '组' }}</td>
                    <td class="text-nowrap small">{{ new Date().toISOString().replace('T', ' ').slice(0, 19) }}</td>
                    <td><button class="btn btn-outline-danger btn-sm" @click="deleteGroup(g.id)">删除</button></td>
                  </tr>
                  <tr v-if="!groups.length"><td colspan="5" class="text-center text-secondary py-4">暂无客户组</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

    </template>
  </AdminShell>
</template>
