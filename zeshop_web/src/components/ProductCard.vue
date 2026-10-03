<script setup lang="ts">
import { computed, ref } from 'vue'
import { navigate } from '../router'
import { addToCart, formatPrice, isFavorite, toggleFavorite, type Product } from '../store'

const props = defineProps<{ product: Product }>()
const adding = ref(false)
const soldOut = computed(() => (props.product.stock ?? 0) <= 0)

async function quickAdd(): Promise<void> {
  adding.value = true
  try {
    await addToCart(props.product, 1)
  } finally {
    adding.value = false
  }
}
</script>

<!-- 结构对齐 BeikeShop 主题 shared/product.blade.php 的商品卡 -->
<template>
  <div class="product-wrap">
    <div class="image">
      <a @click="navigate('/products/' + product.id)">
        <img :src="product.image || ''" :alt="product.title" class="img-fluid image-before">
      </a>
      <div class="button-wrap">
        <button class="btn btn-dark text-light btn-add-cart" type="button" :disabled="adding || soldOut" @click.stop="quickAdd">
          <i class="bi bi-cart"></i> {{ soldOut ? '缺货' : '加入购物车' }}
        </button>
        <button class="btn btn-dark text-light btn-quick-view" type="button" title="快速查看" @click.stop="navigate('/products/' + product.id)">
          <i class="bi bi-eye"></i>
        </button>
        <button class="btn btn-dark text-light btn-wishlist" type="button" title="加入收藏" @click.stop="toggleFavorite(product)">
          <i class="bi" :class="isFavorite(product.id) ? 'bi-heart-fill' : 'bi-heart'"></i>
        </button>
      </div>
    </div>
    <div class="product-bottom-info">
      <div class="product-name" @click="navigate('/products/' + product.id)">{{ product.title }}</div>
      <div class="product-price">
        <span class="price-new">{{ formatPrice(product.price) }}</span>
        <span v-if="product.originPrice && product.originPrice > product.price" class="price-old">{{ formatPrice(product.originPrice) }}</span>
      </div>
    </div>
  </div>
</template>
