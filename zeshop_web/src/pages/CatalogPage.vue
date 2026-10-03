<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import ProductCard from '../components/ProductCard.vue'
import { navigate, query, route } from '../router'
import { state } from '../store'

const keyword = ref(query.value.get('keyword') || '')
const category = ref(query.value.get('category') || '')
const brand = ref(query.value.get('brand') || '')
const minPrice = ref(query.value.get('min_price') || '')
const maxPrice = ref(query.value.get('max_price') || '')
const sort = ref(query.value.get('sort') || '')
const perPage = ref(Number(query.value.get('per_page') || 12))
const pageNo = ref(Number(query.value.get('page') || 1))
const styleList = ref(query.value.get('style_list') || 'grid')
const filterOpen = ref(false)
const perPages = [12, 24, 36]

/** 商品接口只按 category/brand/max_price/q 过滤，最低价、排序与分页在本地完成。 */
const filtered = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  const min = minPrice.value ? Number(minPrice.value) : null
  const max = maxPrice.value ? Number(maxPrice.value) : null
  let items = state.products.filter((item) => {
    if (category.value && item.category !== category.value) return false
    if (brand.value && item.brand !== brand.value) return false
    if (min !== null && item.price < min) return false
    if (max !== null && item.price > max) return false
    if (word && ![item.title, item.brand, item.category, item.description].join(' ').toLowerCase().includes(word)) return false
    return true
  })
  if (sort.value === 'price-asc') items = [...items].sort((a, b) => a.price - b.price)
  else if (sort.value === 'price-desc') items = [...items].sort((a, b) => b.price - a.price)
  else if (sort.value === 'name-asc') items = [...items].sort((a, b) => a.title.localeCompare(b.title, 'zh'))
  else if (sort.value === 'name-desc') items = [...items].sort((a, b) => b.title.localeCompare(a.title, 'zh'))
  else if (sort.value === 'sales-desc') items = [...items].sort((a, b) => (b.sales ?? 0) - (a.sales ?? 0))
  return items
})
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / perPage.value)))
const pageItems = computed(() => filtered.value.slice((pageNo.value - 1) * perPage.value, pageNo.value * perPage.value))
const allCategories = computed(() => [...new Set(state.products.map((item) => item.category).filter(Boolean))] as string[])
/** 侧栏分类按源项目规范分组成树：组内子项就是后端真实分类字段。 */
const categoryTree = computed(() => [
  { name: '服饰', children: ['女装', '男装', '上装', '下装'] },
  { name: '鞋履', children: ['鞋履'] },
  { name: '配饰', children: ['配饰'] },
  { name: '运动户外', children: ['运动户外'] },
  { name: '奢华甄选', children: ['奢华鞋履', '奢华配饰', '奢华腕表', '奢华服饰'] },
].map((group) => ({ ...group, children: group.children.filter((child) => allCategories.value.includes(child)) })).filter((group) => group.children.length))
const openGroups = ref<string[]>([])
function toggleGroup(name: string): void {
  openGroups.value = openGroups.value.includes(name) ? openGroups.value.filter((item) => item !== name) : [...openGroups.value, name]
}
const priceBounds = computed(() => {
  const prices = state.products.map((item) => item.price)
  return { min: Math.floor(Math.min(...prices) || 0), max: Math.ceil(Math.max(...prices) || 0) }
})
/** 价格条的橙色区间按当前「从/到」在整体价位中的比例绘制。 */
const sliderStyle = computed(() => {
  const span = Math.max(1, priceBounds.value.max - priceBounds.value.min)
  const lo = minPrice.value ? Number(minPrice.value) : priceBounds.value.min
  const hi = maxPrice.value ? Number(maxPrice.value) : priceBounds.value.max
  const left = Math.min(100, Math.max(0, ((lo - priceBounds.value.min) / span) * 100))
  const right = Math.min(100, Math.max(0, ((hi - priceBounds.value.min) / span) * 100))
  return { left: left + '%', width: Math.max(3, right - left) + '%' }
})
const allBrands = computed(() => [...new Set(state.products.map((item) => item.brand).filter(Boolean))] as string[])

/** 筛选只改地址栏，不新增历史记录，避免点几次筛选后要按很多次返回。 */
function syncQuery(): void {
  const params = new URLSearchParams()
  if (keyword.value) params.set('keyword', keyword.value)
  if (category.value) params.set('category', category.value)
  if (brand.value) params.set('brand', brand.value)
  if (minPrice.value) params.set('min_price', minPrice.value)
  if (maxPrice.value) params.set('max_price', maxPrice.value)
  if (sort.value) params.set('sort', sort.value)
  if (perPage.value !== 12) params.set('per_page', String(perPage.value))
  if (pageNo.value > 1) params.set('page', String(pageNo.value))
  if (styleList.value !== 'grid') params.set('style_list', styleList.value)
  const qs = params.toString()
  window.history.replaceState({}, '', '/products' + (qs ? '?' + qs : ''))
  route.value = window.location.pathname + window.location.search
}
function applyFilters(): void { pageNo.value = 1; syncQuery() }
function toggleCategory(value: string): void { category.value = category.value === value ? '' : value; applyFilters() }
function toggleBrand(value: string): void { brand.value = brand.value === value ? '' : value; applyFilters() }
function goPage(next: number): void { if (next < 1 || next > totalPages.value) return; pageNo.value = next; syncQuery() }
function reset(): void {
  keyword.value = ''; category.value = ''; brand.value = ''; minPrice.value = ''; maxPrice.value = ''
  sort.value = ''; perPage.value = 12; applyFilters()
}

watch(route, () => {
  keyword.value = query.value.get('keyword') || ''
  category.value = query.value.get('category') || ''
  brand.value = query.value.get('brand') || ''
  minPrice.value = query.value.get('min_price') || ''
  maxPrice.value = query.value.get('max_price') || ''
  sort.value = query.value.get('sort') || ''
  perPage.value = Number(query.value.get('per_page') || 12)
  pageNo.value = Number(query.value.get('page') || 1)
})
</script>

<!-- 结构对齐 BeikeShop 主题 category.blade.php：面包屑 + 左筛选栏 + 右商品区 -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid">
      <nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span>全部商品</span><template v-if="category"><span class="px-2">/</span><span class="text-dark">{{ category }}</span></template><template v-else-if="brand"><span class="px-2">/</span><span class="text-dark">{{ brand }}</span></template></nav>
    </div>
    <div class="mb-filter" @click="filterOpen = true"><i class="bi bi-funnel"></i></div>
  </div>

  <div class="container-fluid">
    <div class="row">
      <div class="col-12 col-lg-3 pe-lg-4 left-column" :class="{ 'd-block': filterOpen }">
        <div class="x-fixed-top" :class="{ active: filterOpen }">
          <div class="mb-4 module-category-wrap">
            <h4 class="mb-3"><span>分类</span></h4>
            <ul class="sidebar-widget mb-0" id="category-one">
              <li :class="{ active: !category }"><a class="category-href" @click="category = ''; applyFilters()">全部商品</a></li>
              <li v-for="group in categoryTree" :key="group.name" :class="{ active: group.children.includes(category) }">
                <a class="category-href" @click="toggleGroup(group.name)">{{ group.name }}</a>
                <button class="toggle-icon btn" type="button" :class="{ collapsed: !openGroups.includes(group.name) }" @click.stop="toggleGroup(group.name)"><i class="bi bi-chevron-up"></i></button>
                <ul v-show="openGroups.includes(group.name)" class="accordion-collapse collapse show">
                  <li v-for="child in group.children" :key="child" class="child-category" :class="{ active: category === child }">
                    <a class="category-href" @click="category = child; applyFilters()">{{ child }}</a>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
          <div class="filter-box">
            <div class="card">
              <div class="card-header p-0"><h4 class="mb-3">价格</h4></div>
              <div class="card-body p-0">
                <div id="price-slider" class="mb-2"><div class="slider-bg" :style="sliderStyle"></div></div>
                <div class="text-secondary price-range d-flex justify-content-between">
                  <div class="d-flex align-items-center wp-100">从<span class="min ms-1 input-group-sm"><input v-model="minPrice" type="text" class="form-control price-select-min"></span></div>
                  <div class="d-flex align-items-center wp-100">到<span class="max ms-1 input-group-sm"><input v-model="maxPrice" type="text" class="form-control price-select-max"></span></div>
                </div>
                <button class="btn btn-dark btn-sm w-100 mt-3" type="button" @click="applyFilters">确定</button>
                <button class="btn btn-outline-secondary btn-sm w-100 mt-2" type="button" @click="reset()">重置全部筛选</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-9 right-column">
        <div v-if="category || brand" class="category-intro mb-3 p-3 border rounded-3 border-light-subtle">
          <div class="row g-4 align-items-center">
            <div class="col-auto"><div class="category-avatar wh-150 d-flex align-items-center">{{ (category || brand).slice(0, 1) }}</div></div>
            <div class="col"><div class="category-desc"><p class="mb-2 fs-3 fw-bold">{{ category || brand }}</p><p class="mb-0 lh-base opacity-75">共 {{ filtered.length }} 件商品，价格与库存实时来自数据库。</p></div></div>
          </div>
        </div>

        <div v-if="pageItems.length" class="product-tool d-flex justify-content-between align-items-center mb-lg-4 mb-2">
          <div class="style-wrap d-flex align-items-center">
            <label :class="{ active: styleList === 'grid' }" class="grid" title="网格" @click="styleList = 'grid'; applyFilters()"><svg viewBox="0 0 19 19" xmlns="http://www.w3.org/2000/svg" width="18" height="18"><rect width="5" height="5"></rect><rect x="7" width="5" height="5"></rect><rect x="14" width="5" height="5"></rect><rect y="7" width="5" height="5"></rect><rect x="7" y="7" width="5" height="5"></rect><rect x="14" y="7" width="5" height="5"></rect><rect y="14" width="5" height="5"></rect><rect x="7" y="14" width="5" height="5"></rect><rect x="14" y="14" width="5" height="5"></rect></svg></label>
            <label :class="{ active: styleList === 'list' }" class="ms-1 list" title="列表" @click="styleList = 'list'; applyFilters()"><svg viewBox="0 0 19 19" xmlns="http://www.w3.org/2000/svg" width="18" height="18"><rect width="5" height="5"></rect><rect x="7" width="12" height="5"></rect><rect y="7" width="5" height="5"></rect><rect x="7" y="7" height="5" width="12"></rect><rect y="14" width="5" height="5"></rect><rect x="7" y="14" height="5" width="12"></rect></svg></label>
          </div>
          <div class="d-flex align-items-center right-per-page">
            <div class="text-nowrap text-secondary">共 {{ filtered.length }} 件商品</div>
            <select v-model.number="perPage" class="form-select perpage-select ms-3" @change="applyFilters">
              <option v-for="value in perPages" :key="value" :value="value">{{ value }}</option>
            </select>
            <select v-model="brand" class="form-select ms-2" style="width:auto" @change="applyFilters">
              <option value="">全部品牌</option>
              <option v-for="item in allBrands" :key="item" :value="item">{{ item }}</option>
            </select>
            <select v-model="sort" class="form-select order-select ms-2" @change="applyFilters">
              <option value="">默认</option>
              <option value="sales-desc">销量（高-低）</option>
              <option value="name-asc">名称（A - Z）</option>
              <option value="name-desc">名称（Z - A）</option>
              <option value="price-asc">价格（低-高）</option>
              <option value="price-desc">价格（高-低）</option>
            </select>
          </div>
        </div>

        <div v-if="pageItems.length" class="row g-3 g-lg-4" :class="{ 'product-list-wrap': styleList === 'list' }">
          <div v-for="item in pageItems" :key="item.id" :class="styleList === 'list' ? 'col-12' : 'product-grid col-6 col-md-3'"><ProductCard :product="item" /></div>
        </div>
        <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-muted">
          <i class="bi bi-inbox fs-1 mb-2"></i><p>没有找到符合条件的商品</p>
        </div>

        <nav v-if="totalPages > 1" class="mt-4">
          <ul class="pagination justify-content-center">
            <li class="page-item" :class="{ disabled: pageNo <= 1 }"><a class="page-link" @click="goPage(pageNo - 1)">上一页</a></li>
            <li v-for="n in totalPages" :key="n" class="page-item" :class="{ active: n === pageNo }"><a class="page-link" @click="goPage(n)">{{ n }}</a></li>
            <li class="page-item" :class="{ disabled: pageNo >= totalPages }"><a class="page-link" @click="goPage(pageNo + 1)">下一页</a></li>
          </ul>
        </nav>
      </div>
    </div>
  </div>
</template>
