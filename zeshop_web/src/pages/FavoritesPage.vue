<script setup lang="ts">
import { computed } from 'vue'
import AccountSidebar from '../components/AccountSidebar.vue'
import ProductCard from '../components/ProductCard.vue'
import { navigate } from '../router'
import { state } from '../store'

const items = computed(() => state.products.filter((product) => state.favorites.includes(product.id)))
</script>

<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span class="text-dark">我的收藏</span></nav></div>
  </div>

  <div class="container">
    <div class="row">
      <AccountSidebar active="favorites" />
      <div class="col-12 col-md-9">
        <div class="card mb-4 account-card order-wrap h-min-600">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h5 class="card-title">我的收藏</h5>
            <span class="text-muted">共 {{ items.length }} 件</span>
          </div>
          <div class="card-body">
            <div v-if="items.length" class="row g-3 g-lg-4">
              <div v-for="item in items" :key="item.id" class="product-grid col-6 col-md-4"><ProductCard :product="item" /></div>
            </div>
            <div v-else class="d-flex flex-column align-items-center justify-content-center py-5 text-muted">
              <i class="bi bi-heart fs-1 mb-2"></i>
              <p class="mb-0">还没有收藏商品，点商品卡上的心形按钮即可收藏</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
