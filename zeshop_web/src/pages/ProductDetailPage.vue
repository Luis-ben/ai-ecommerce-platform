<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import ProductCard from '../components/ProductCard.vue'
import { navigate } from '../router'
import { addToCart, formatPrice, isFavorite, loadProduct, state, toggleFavorite, type Product } from '../store'

const props = defineProps<{ id: string }>()
const product = ref<Product | null>(null)
const quantity = ref(1)
const loading = ref(true)
const adding = ref(false)
const tab = ref(0)

const soldOut = computed(() => (product.value?.stock ?? 0) <= 0)
const attributes = computed(() => {
  const item = product.value
  if (!item) return []
  return [
    { name: '品牌', value: item.brand || '—' },
    { name: '分类', value: item.category || '—' },
    { name: '库存', value: String(item.stock ?? 0) },
    { name: '货号', value: item.id },
    { name: '币种', value: 'CNY' },
  ]
})
/** 相关商品取同分类的在售商品，对应主题的 relations-wrap 区块。 */
const relations = computed(() => state.products.filter((item) => item.id !== props.id && item.category === product.value?.category && (item.stock ?? 0) > 0).slice(0, 4))

async function load(id: string): Promise<void> {
  loading.value = true
  product.value = await loadProduct(id)
  quantity.value = 1
  tab.value = 0
  loading.value = false
}
watch(() => props.id, (id) => { void load(id) }, { immediate: true })

async function add(redirect: boolean): Promise<void> {
  if (!product.value) return
  adding.value = true
  try {
    const ok = await addToCart(product.value, quantity.value)
    if (ok && redirect) navigate('/checkout')
  } finally {
    adding.value = false
  }
}
</script>

<!-- 结构对齐 BeikeShop 主题 product/product.blade.php -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid">
      <nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><a @click="navigate('/products')">全部商品</a><template v-if="product?.category"><span class="px-2">/</span><a @click="navigate('/products?category=' + encodeURIComponent(product.category))">{{ product.category }}</a></template><span class="px-2">/</span><span class="text-dark">{{ product?.title || '商品详情' }}</span></nav>
    </div>
  </div>

  <div class="container product-container">
    <div v-if="loading" class="text-center py-5 text-muted">加载中…</div>
    <div v-else-if="!product" class="text-center py-5">
      <p class="text-muted">商品不存在或已下架</p>
      <button class="btn btn-dark" @click="navigate('/products')">返回商品列表</button>
    </div>
    <template v-else>
      <div class="row mb-md-5 mt-md-0" id="product-top">
        <div class="col-12 col-lg-6 mb-2">
          <div class="product-image">
            <div class="left">
              <div class="swiper product-left-thumb-wrap"><div class="swiper-wrapper"><div class="swiper-slide active"><a><img :src="product.image || ''" :alt="product.title" class="img-fluid seo-img" width="120" height="120"></a></div></div></div>
            </div>
            <div class="right" id="zoom"><div class="product-img"><img :src="product.image || ''" :alt="product.title" class="img-fluid seo-img"></div></div>
          </div>
        </div>
        <div class="col-12 col-lg-6">
          <div class="product-info product-mb-block" id="product-app">
            <h1 class="mb-lg-4 mb-2 product-name">{{ product.title }}</h1>
            <div class="price-wrap d-flex align-items-end">
              <div class="new-price fs-1 lh-1 fw-bold me-2">{{ formatPrice(product.price) }}</div>
              <div v-if="product.originPrice && product.originPrice > product.price" class="old-price text-muted text-decoration-line-through">{{ formatPrice(product.originPrice) }}</div>
            </div>
            <div class="stock-and-sku mb-lg-4 mb-2 mt-3">
              <div class="d-lg-flex"><span class="title text-muted">库存:</span><span :class="soldOut ? 'text-secondary' : 'text-success'">{{ soldOut ? '缺货' : '有货（' + (product.stock ?? 0) + ' 件）' }}</span></div>
              <div v-if="product.brand" class="d-lg-flex"><span class="title text-muted">品牌:</span><a @click="navigate('/products?brand=' + encodeURIComponent(product.brand || ''))">{{ product.brand }}</a></div>
              <div class="d-lg-flex"><span class="title text-muted">SKU:</span>{{ product.id }}</div>
            </div>
            <div class="mb-md-3">
              <p class="mb-2">数量:</p>
              <div class="input-group quantity-wrap">
                <button class="btn quantity-reduce" type="button" @click="quantity = Math.max(1, quantity - 1)"><i class="bi bi-dash-lg"></i></button>
                <input v-model.number="quantity" type="text" class="form-control" min="1" :max="product.stock || 1">
                <button class="btn quantity-increase" type="button" @click="quantity = quantity + 1"><i class="bi bi-plus-lg"></i></button>
              </div>
            </div>
            <div class="product-btns">
              <div class="add-cart-btns">
                <button class="btn btn-outline-dark add-cart fw-bold" type="button" :disabled="adding || soldOut" @click="add(false)"><i class="bi bi-cart-fill me-1"></i>加入购物车</button>
                <button class="btn btn-dark ms-md-3 btn-buy-now fw-bold" type="button" :disabled="adding || soldOut" @click="add(true)"><i class="bi bi-bag-fill me-1"></i>立即购买</button>
              </div>
              <div class="add-wishlist">
                <button class="btn btn-link ps-md-0 text-secondary" type="button" @click="toggleFavorite(product)">
                  <i class="bi me-1" :class="isFavorite(product.id) ? 'bi-heart-fill' : 'bi-heart'"></i><span>{{ isFavorite(product.id) ? '已收藏' : '加入收藏' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="product-description product-mb-block">
        <div class="nav nav-tabs nav-overflow justify-content-start justify-content-md-center border-bottom mb-3">
          <a class="nav-link fw-bold fs-5" :class="{ active: tab === 0 }" @click="tab = 0">商品详情</a>
          <a class="nav-link fw-bold fs-5" :class="{ active: tab === 1 }" @click="tab = 1">商品参数</a>
        </div>
        <div class="tab-content">
          <div v-show="tab === 0" class="tab-pane fade show active"><div class="rich-text-editor-content"><p>{{ product.description }}</p></div></div>
          <div v-show="tab === 1" class="tab-pane fade"><table class="table table-bordered attribute-table"><tbody><tr v-for="attr in attributes" :key="attr.name"><td style="width:200px">{{ attr.name }}</td><td>{{ attr.value }}</td></tr></tbody></table></div>
        </div>
      </div>
    </template>
  </div>

  <div v-if="relations.length" class="relations-wrap mt-2 mt-md-5 product-mb-block">
    <div class="container position-relative">
      <div class="title text-center">相关商品</div>
      <div class="row g-3 g-lg-4 mt-2">
        <div v-for="item in relations" :key="item.id" class="product-grid col-6 col-md-3"><ProductCard :product="item" /></div>
      </div>
    </div>
  </div>
</template>
