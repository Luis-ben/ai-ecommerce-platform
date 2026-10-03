<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminShell from './AdminShell.vue'
import { api } from '../api'
import { isStaff, notify } from '../store'

type Article = { id: string; title: string; category?: string; status: string; views: number; created_at: string; updated_at?: string }
type ArticleCategory = { id: string; title: string; parent?: string; status: string; created_at: string; updated_at?: string }

const subPage = ref<'list' | 'categories'>('list')
const articles = ref<Article[]>([])
const categories = ref<ArticleCategory[]>([])
const loading = ref(true)
const filterCategory = ref('')

// 新建
const newTitle = ref('')
const newCategory = ref('')
const newCatTitle = ref('')

async function load(): Promise<void> {
  loading.value = true
  try {
    const [ad, cd] = await Promise.all([
      api<{ items: Article[] }>('/api/admin/articles').catch(() => ({ items: [] as Article[] })),
      api<{ items: ArticleCategory[] }>('/api/admin/article-categories').catch(() => ({ items: [] as ArticleCategory[] })),
    ])
    articles.value = ad.items
    categories.value = cd.items
  } catch (error) { notify((error as Error).message) }
  finally { loading.value = false }
}

async function createArticle(): Promise<void> {
  if (!newTitle.value.trim()) { notify('请填写文章标题'); return }
  try {
    await api('/api/admin/articles', { method: 'POST', body: JSON.stringify({ title: newTitle.value.trim(), category: newCategory.value }) })
    notify('文章已创建'); newTitle.value = ''; await load()
  } catch (error) { notify((error as Error).message) }
}

async function deleteArticle(id: string): Promise<void> {
  if (!confirm('确定删除该文章？')) return
  try { await api('/api/admin/articles/' + id, { method: 'DELETE' }); notify('已删除'); await load() }
  catch (error) { notify((error as Error).message) }
}

async function createCategory(): Promise<void> {
  if (!newCatTitle.value.trim()) { notify('请填写分类标题'); return }
  try {
    await api('/api/admin/article-categories', { method: 'POST', body: JSON.stringify({ title: newCatTitle.value.trim() }) })
    notify('分类已创建'); newCatTitle.value = ''; await load()
  } catch (error) { notify((error as Error).message) }
}

async function deleteCat(id: string): Promise<void> {
  if (!confirm('确定删除该分类？')) return
  try { await api('/api/admin/article-categories/' + id, { method: 'DELETE' }); notify('已删除'); await load() }
  catch (error) { notify((error as Error).message) }
}

onMounted(load)
</script>

<template>
  <AdminShell active="articles" :title="subPage === 'categories' ? '文章分类' : '文章管理'">
    <p v-if="!isStaff" class="alert alert-warning">该页面需要商户角色，请以 merchant@example.com 登录。</p>
    <template v-else>
      <!-- 子页面切换 -->
      <div class="d-flex gap-2 mb-4">
        <button class="btn btn-sm" :class="subPage === 'list' ? 'btn-primary' : 'btn-outline-secondary'" @click="subPage = 'list'">文章管理</button>
        <button class="btn btn-sm" :class="subPage === 'categories' ? 'btn-primary' : 'btn-outline-secondary'" @click="subPage = 'categories'">文章分类</button>
      </div>

      <!-- ── 文章列表 ── -->
      <template v-if="subPage === 'list'">
        <div class="card h-min-600">
          <div class="card-body">
            <!-- 筛选 -->
            <div class="bg-light rounded-3 p-3 mb-4 d-flex gap-3 align-items-end">
              <div>
                <label class="filter-title">分类</label>
                <select v-model="filterCategory" class="form-select">
                  <option value="">全部</option>
                  <option v-for="c in categories" :key="c.id" :value="c.title">{{ c.title }}</option>
                </select>
              </div>
              <button class="btn btn-primary btn-sm" @click="load">筛选</button>
              <button class="btn btn-outline-secondary btn-sm" @click="filterCategory = ''">重置</button>
            </div>

            <!-- 新建 -->
            <div class="d-flex gap-2 mb-4 align-items-end">
              <div class="flex-grow-1">
                <input v-model="newTitle" class="form-control" placeholder="文章标题（回车创建）" @keyup.enter="createArticle" />
              </div>
              <select v-model="newCategory" class="form-select" style="width:160px">
                <option value="">无分类</option>
                <option v-for="c in categories" :key="c.id" :value="c.title">{{ c.title }}</option>
              </select>
              <button class="btn btn-primary btn-sm" @click="createArticle"><i class="bi bi-plus-lg me-1"></i>添加</button>
            </div>

            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <div v-else class="table-push">
              <table class="table table-hover align-middle">
                <thead>
                  <tr><th>ID</th><th>标题</th><th>文章分类</th><th>状态</th><th>查看数</th><th>创建时间</th><th>修改时间</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(a, i) in articles.filter(x => !filterCategory || x.category === filterCategory)" :key="a.id">
                    <td>{{ i + 1 }}</td>
                    <td>{{ a.title }}</td>
                    <td>{{ a.category || '-' }}</td>
                    <td><span class="text-success small">启用</span></td>
                    <td>{{ a.views || 0 }}</td>
                    <td class="text-nowrap small">{{ a.created_at.replace('T', ' ').slice(0, 19) }}</td>
                    <td class="text-nowrap small">{{ (a.updated_at || a.created_at).replace('T', ' ').slice(0, 19) }}</td>
                    <td class="text-nowrap">
                      <button class="btn btn-outline-danger btn-sm me-1" @click="deleteArticle(a.id)">删除</button>
                      <button class="btn btn-outline-secondary btn-sm"><i class="bi bi-eye"></i></button>
                    </td>
                  </tr>
                  <tr v-if="!articles.length">
                    <td colspan="8" class="text-center text-secondary py-4">暂无文章，先添加一篇吧</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

      <!-- ── 文章分类 ── -->
      <template v-else>
        <div class="card h-min-600">
          <div class="card-body">
            <div class="d-flex gap-2 mb-4 align-items-end">
              <div>
                <input v-model="newCatTitle" class="form-control" placeholder="分类标题" @keyup.enter="createCategory" />
              </div>
              <button class="btn btn-primary btn-sm" @click="createCategory"><i class="bi bi-plus-lg me-1"></i>添加</button>
            </div>
            <div v-if="loading" class="text-center py-5 text-secondary">加载中…</div>
            <div v-else class="table-push">
              <table class="table table-hover align-middle">
                <thead><tr><th>ID</th><th>标题</th><th>上级分类</th><th>状态</th><th>创建时间</th><th>修改时间</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="(c, i) in categories" :key="c.id">
                    <td>{{ i + 1 }}</td>
                    <td>{{ c.title }}</td>
                    <td>-</td>
                    <td><span class="text-success small">启用</span></td>
                    <td class="text-nowrap small">{{ c.created_at.replace('T', ' ').slice(0, 19) }}</td>
                    <td class="text-nowrap small">{{ (c.updated_at || c.created_at).replace('T', ' ').slice(0, 19) }}</td>
                    <td class="text-nowrap">
                      <button class="btn btn-outline-danger btn-sm me-1" @click="deleteCat(c.id)">删除</button>
                      <button class="btn btn-outline-secondary btn-sm"><i class="bi bi-eye"></i></button>
                    </td>
                  </tr>
                  <tr v-if="!categories.length">
                    <td colspan="7" class="text-center text-secondary py-4">暂无分类</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

    </template>
  </AdminShell>
</template>
