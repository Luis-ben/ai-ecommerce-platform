<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { navigate } from '../router'
import { formatPrice, isStaff, notify, state, type Product } from '../store'

const products = ref<Product[]>([])
const draft = reactive({ title: '', description: '', category: '女装', brand: '', image: '/image/catalog/demo/product/1.webp', price: 199, stock: 10, active: true })
const editing = ref<Record<string, { price: number; stock: number }>>({})

async function load(): Promise<void> {
  const data = await api<{ items: Product[] }>('/api/products')
  products.value = data.items
  editing.value = Object.fromEntries(data.items.map((item) => [item.id, { price: item.price, stock: item.stock ?? 0 }]))
}

async function create(): Promise<void> {
  try {
    await api('/api/products', { method: 'POST', body: JSON.stringify(draft) })
    notify('商品已创建')
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

async function save(item: Product): Promise<void> {
  const next = editing.value[item.id]
  try {
    await api('/api/products/' + item.id, { method: 'PUT', body: JSON.stringify({ title: item.title, description: item.description || '', category: item.category || '', brand: item.brand || '', image: item.image || null, price: next.price, stock: next.stock, active: true }) })
    notify('已保存 ' + item.title)
    await load()
  } catch (error) {
    notify((error as Error).message)
  }
}

onMounted(() => { void load() })
</script>

<template>
  <div class="container-fluid py-4">
    <nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span>商品管理</span></nav>
    <h1 class="fs-4 mb-3">商品管理</h1>
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户或客服角色，请用 merchant@example.com 登录。</p>
    <template v-else>
      <div class="border p-3 mb-4">
        <h2 class="fs-6">新增商品</h2>
        <div class="row g-2">
          <div class="col-12 col-md-4"><input v-model="draft.title" class="form-control" placeholder="商品标题"></div>
          <div class="col-6 col-md-2"><input v-model="draft.brand" class="form-control" placeholder="品牌"></div>
          <div class="col-6 col-md-2"><input v-model="draft.category" class="form-control" placeholder="分类"></div>
          <div class="col-6 col-md-2"><input v-model.number="draft.price" class="form-control" type="number" placeholder="价格"></div>
          <div class="col-6 col-md-2"><input v-model.number="draft.stock" class="form-control" type="number" placeholder="库存"></div>
          <div class="col-12"><textarea v-model="draft.description" class="form-control" rows="2" placeholder="商品描述"></textarea></div>
          <div class="col-12"><button class="btn btn-dark" :disabled="state.busy" @click="create">创建商品</button></div>
        </div>
      </div>
      <div class="table-responsive">
        <table class="table align-middle">
          <thead><tr><th>商品</th><th>品牌</th><th>分类</th><th>价格</th><th>库存</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in products" :key="item.id">
              <td class="d-flex align-items-center gap-2"><img :src="item.image || ''" class="ze-row-thumb" :alt="item.title"><span>{{ item.title }}</span></td>
              <td>{{ item.brand }}</td>
              <td>{{ item.category }}</td>
              <td><input v-model.number="editing[item.id].price" class="form-control form-control-sm" style="width:110px" type="number"></td>
              <td><input v-model.number="editing[item.id].stock" class="form-control form-control-sm" style="width:90px" type="number"></td>
              <td><button class="btn btn-sm btn-outline-dark" @click="save(item)">保存</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="small text-secondary">列表来自 GET /api/products（仅在售且有库存的商品）；保存走 PUT /api/products/{{ '{' }}id{{ '}' }}。</p>
    </template>
  </div>
</template>
