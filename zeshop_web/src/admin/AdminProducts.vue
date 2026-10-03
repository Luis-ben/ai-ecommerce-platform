<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify, state, type Product } from '../store'

const products = ref<Product[]>([])
const loading = ref(true)
const filter = reactive({ name: '', sku: '', brand: '', category: '', status: '' })
const pageNo = ref(1)
const perPage = 10
const editing = ref<Record<string, { price: number; stock: number; active: boolean }>>({})
const selectedIds = ref<string[]>([])
const draftOpen = ref(false)
const draft = reactive({
  title: '',
  description: '',
  category: '女装',
  brand: 'Gucci',
  image: '/image/catalog/demo/product/1.webp',
  price: 199,
  stock: 10,
  active: true,
})

const filtered = computed(() => products.value.filter((item) => {
  if (filter.name && !item.title.toLowerCase().includes(filter.name.toLowerCase())) return false
  if (filter.sku && !item.id.includes(filter.sku.trim())) return false
  if (filter.brand && item.brand !== filter.brand) return false
  if (filter.category && item.category !== filter.category) return false
  if (filter.status === 'active' && editing.value[item.id]?.active === false) return false
  if (filter.status === 'inactive' && editing.value[item.id]?.active !== false) return false
  return true
}))

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / perPage)))
const pageItems = computed(() => filtered.value.slice((pageNo.value - 1) * perPage, pageNo.value * perPage))
const allCategories = computed(() => [...new Set(products.value.map((item) => item.category).filter(Boolean))] as string[])
const allBrands = computed(() => [...new Set(products.value.map((item) => item.brand).filter(Boolean))] as string[])

const allSelected = computed({
  get: () => pageItems.value.length > 0 && pageItems.value.every((p) => selectedIds.value.includes(p.id)),
  set: (val: boolean) => {
    if (val) {
      const ids = new Set([...selectedIds.value, ...pageItems.value.map((p) => p.id)])
      selectedIds.value = [...ids]
    } else {
      const pageIdSet = new Set(pageItems.value.map((p) => p.id))
      selectedIds.value = selectedIds.value.filter((id) => !pageIdSet.has(id))
    }
  },
})

function toggleSelect(id: string): void {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await api<{ items: Product[] }>('/api/products')
    products.value = data.items
    editing.value = Object.fromEntries(
      data.items.map((item) => [
        item.id,
        { price: item.price, stock: item.stock ?? 0, active: item.active !== false },
      ])
    )
  } catch (error) {
    notify((error as Error).message)
  } finally {
    loading.value = false
  }
}

function reset(): void {
  filter.name = ''; filter.sku = ''; filter.brand = ''; filter.category = ''; filter.status = ''
  pageNo.value = 1
}

function goPage(next: number): void {
  if (next < 1 || next > totalPages.value) return
  pageNo.value = next
}

async function createProduct(): Promise<void> {
  try {
    await api('/api/products', { method: 'POST', body: JSON.stringify(draft) })
    notify('商品已创建')
    draftOpen.value = false
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

async function save(item: Product): Promise<void> {
  const next = editing.value[item.id]
  try {
    await api('/api/products/' + item.id, {
      method: 'PUT',
      body: JSON.stringify({
        title: item.title,
        description: item.description || '',
        category: item.category || '',
        brand: item.brand || '',
        image: item.image || null,
        price: next.price,
        stock: next.stock,
        active: next.active,
      }),
    })
    notify('已保存 ' + item.title)
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

async function toggleProductStatus(item: Product): Promise<void> {
  editing.value[item.id].active = !editing.value[item.id].active
  await save(item)
}

async function batchDelete(): Promise<void> {
  if (!selectedIds.value.length) { notify('请先勾选商品'); return }
  if (!confirm(`确定删除选中的 ${selectedIds.value.length} 件商品？`)) return
  for (const id of selectedIds.value) {
    try { await api('/api/products/' + id, { method: 'DELETE' }) } catch {}
  }
  selectedIds.value = []
  notify('批量删除成功')
  await load()
}

async function batchStatus(active: boolean): Promise<void> {
  if (!selectedIds.value.length) { notify('请先勾选商品'); return }
  for (const id of selectedIds.value) {
    if (editing.value[id]) {
      editing.value[id].active = active
      const p = products.value.find((x) => x.id === id)
      if (p) await save(p)
    }
  }
  notify(`已批量${active ? '上架' : '下架'}`)
  await load()
}

onMounted(load)
</script>

<!-- 对齐 BeikeShop 官方后台 商品管理 (img_4.png) -->
<template>
  <AdminShell active="products" active-child="/admin/products" title="商品管理">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请用 merchant@example.com 登录。</p>
    <template v-else>
      <div class="card h-min-600">
        <div class="card-body">
          <!-- 筛选栏 (img_4.png) -->
          <div class="bg-light rounded-3 p-4 mb-4">
            <div class="row g-3 align-items-end">
              <div class="col-12 col-md-3">
                <label class="filter-title">名称</label>
                <input v-model="filter.name" class="form-control" placeholder="名称" />
              </div>
              <div class="col-12 col-md-2">
                <label class="filter-title">SKU</label>
                <input v-model="filter.sku" class="form-control" placeholder="SKU" />
              </div>
              <div class="col-12 col-md-2">
                <label class="filter-title">品牌/型号</label>
                <select v-model="filter.brand" class="form-select">
                  <option value="">全部</option>
                  <option v-for="b in allBrands" :key="b" :value="b">{{ b }}</option>
                </select>
              </div>
              <div class="col-12 col-md-2">
                <label class="filter-title">分类</label>
                <select v-model="filter.category" class="form-select">
                  <option value="">请选择</option>
                  <option v-for="c in allCategories" :key="c" :value="c">{{ c }}</option>
                </select>
              </div>
              <div class="col-12 col-md-2">
                <label class="filter-title">状态</label>
                <select v-model="filter.status" class="form-select">
                  <option value="">全部</option>
                  <option value="active">上架</option>
                  <option value="inactive">下架</option>
                </select>
              </div>
              <div class="col-auto">
                <button type="button" class="btn btn-primary btn-sm me-2" @click="pageNo = 1">筛选</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="reset">重置</button>
              </div>
            </div>
          </div>

          <!-- 操作条 (img_4.png: 创建商品、批量删除、批量上架、批量下架) -->
          <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <div class="d-flex gap-2 align-items-center">
              <button
                class="btn btn-warning text-white btn-sm px-3"
                style="background-color: #ff5722; border-color: #ff5722"
                type="button"
                @click="draftOpen = !draftOpen"
              >
                <i class="bi bi-plus-lg me-1"></i>创建商品
              </button>
              <button class="btn btn-outline-danger btn-sm" type="button" @click="batchDelete">批量删除</button>
              <button class="btn btn-outline-success btn-sm" type="button" @click="batchStatus(true)">批量上架</button>
              <button class="btn btn-outline-secondary btn-sm" type="button" @click="batchStatus(false)">批量下架</button>
            </div>
            <span class="text-secondary small">共 {{ filtered.length }} 件商品</span>
          </div>

          <!-- 创建商品抽屉 -->
          <div v-if="draftOpen" class="bg-light border rounded-3 p-4 mb-4">
            <h6 class="fw-bold mb-3">创建新商品</h6>
            <div class="row g-3">
              <div class="col-12 col-md-4">
                <label class="filter-title">名称</label>
                <input v-model="draft.title" class="form-control" placeholder="商品标题" />
              </div>
              <div class="col-6 col-md-2">
                <label class="filter-title">品牌</label>
                <input v-model="draft.brand" class="form-control" />
              </div>
              <div class="col-6 col-md-2">
                <label class="filter-title">分类</label>
                <input v-model="draft.category" class="form-control" />
              </div>
              <div class="col-6 col-md-2">
                <label class="filter-title">价格</label>
                <input v-model.number="draft.price" type="number" class="form-control" />
              </div>
              <div class="col-6 col-md-2">
                <label class="filter-title">库存</label>
                <input v-model.number="draft.stock" type="number" class="form-control" />
              </div>
              <div class="col-12">
                <label class="filter-title">描述</label>
                <textarea v-model="draft.description" class="form-control" rows="2"></textarea>
              </div>
              <div class="col-auto">
                <button class="btn btn-primary btn-sm px-4" type="button" :disabled="state.busy" @click="createProduct">保存</button>
                <button class="btn btn-outline-secondary btn-sm ms-2" type="button" @click="draftOpen = false">取消</button>
              </div>
            </div>
          </div>

          <!-- 商品列表表格 (img_4.png) -->
          <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
          <div v-else-if="pageItems.length" class="table-push">
            <table class="table table-hover align-middle">
              <thead>
                <tr>
                  <th style="width: 40px"><input type="checkbox" v-model="allSelected" /></th>
                  <th style="width: 60px">ID</th>
                  <th style="width: 80px">图片</th>
                  <th>名称</th>
                  <th>销量</th>
                  <th>价格</th>
                  <th>库存</th>
                  <th>状态</th>
                  <th class="text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pageItems" :key="item.id">
                  <td>
                    <input
                      type="checkbox"
                      :checked="selectedIds.includes(item.id)"
                      @change="toggleSelect(item.id)"
                    />
                  </td>
                  <td class="text-secondary small">{{ item.id.slice(-6) }}</td>
                  <td>
                    <div class="border rounded p-1 d-flex align-items-center justify-content-center" style="width: 52px; height: 52px; background: #fff">
                      <img :src="item.image || ''" style="max-width: 44px; max-height: 44px; object-fit: cover" :alt="item.title" />
                    </div>
                  </td>
                  <td>
                    <div class="text-break">
                      <p class="mb-0 fw-medium small" :title="item.title">{{ item.title }}</p>
                      <small class="text-secondary font-monospace">{{ item.id }}</small>
                    </div>
                  </td>
                  <td class="fw-semibold text-secondary">{{ item.sales ?? 0 }}</td>
                  <td>
                    <input
                      v-model.number="editing[item.id].price"
                      type="number"
                      class="form-control form-control-sm"
                      style="width: 100px"
                    />
                  </td>
                  <td>
                    <input
                      v-model.number="editing[item.id].stock"
                      type="number"
                      class="form-control form-control-sm"
                      style="width: 90px"
                    />
                  </td>
                  <td>
                    <!-- 状态 switch 开关 -->
                    <div class="form-check form-switch mb-0">
                      <input
                        class="form-check-input"
                        type="checkbox"
                        :checked="editing[item.id]?.active !== false"
                        @change="toggleProductStatus(item)"
                      />
                    </div>
                  </td>
                  <td class="text-center text-nowrap">
                    <button class="btn btn-outline-primary btn-sm me-1" type="button" @click="save(item)">
                      保存
                    </button>
                    <a :href="'/products/' + item.id" target="_blank" class="btn btn-outline-secondary btn-sm" title="预览商品">
                      <i class="bi bi-eye"></i>
                    </a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-secondary">
            <i class="bi bi-inbox fs-1 mb-2"></i><p class="mb-0">没有符合条件的商品</p>
          </div>

          <nav v-if="totalPages > 1" class="mt-4">
            <ul class="pagination justify-content-end">
              <li class="page-item" :class="{ disabled: pageNo <= 1 }"><a class="page-link" @click="goPage(pageNo - 1)">上一页</a></li>
              <li v-for="n in totalPages" :key="n" class="page-item" :class="{ active: n === pageNo }"><a class="page-link" @click="goPage(n)">{{ n }}</a></li>
              <li class="page-item" :class="{ disabled: pageNo >= totalPages }"><a class="page-link" @click="goPage(pageNo + 1)">下一页</a></li>
            </ul>
          </nav>
        </div>
      </div>
    </template>
  </AdminShell>
</template>
