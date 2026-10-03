<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import ProductCard from '../components/ProductCard.vue'
import { navigate, query } from '../router'
import { brands, categories, state } from '../store'

const activeSlide = ref(0)
const activeTab = ref(0)
const tabs = [
  { key: 'featured', label: '时尚穿搭' },
  { key: 'budget', label: '潮流套装' },
  { key: 'promo', label: '促销专场' },
]
const keyword = computed(() => query.value.get('keyword') || '')
const heroSlides = [
  { image: '/image/catalog/demo/banner/text-image-banner-3.webp', subtitle: '新品上市', title: '全场低至五折，限时开启', description: '下单即享终身质保，品质承诺看得见', link: '/products/35' },
  { image: '/image/catalog/demo/banner/text-image-banner-4.webp', subtitle: '时尚精选', title: '美学盛宴 · 时尚大赏', description: '下单即享终身质保，品质承诺看得见', link: '/products/39' },
]

const inStock = computed(() => state.products.filter((item) => (item.stock ?? 0) > 0))
/** 标签页只是同一批在售商品的不同排序，价格与库存始终来自接口。 */
const tabProducts = computed(() => {
  const items = [...inStock.value]
  if (tabs[activeTab.value].key === 'budget') return items.sort((a, b) => a.price - b.price).slice(0, 8)
  if (tabs[activeTab.value].key === 'promo') return items.sort((a, b) => (b.originPrice ?? 0) - b.price - ((a.originPrice ?? 0) - a.price)).slice(0, 8)
  return items.slice(0, 8)
})
const luxuryProducts = computed(() => inStock.value.filter((item) => item.tag === 'Luxury' || ['Gucci', 'Prada', 'Dior', 'Louis Vuitton', 'Chanel', 'Hermès', 'Cartier', 'Rolex', 'Fendi', 'Loewe'].includes(item.brand || '')).slice(0, 8))
const featured = computed(() => inStock.value.slice(0, 4))

let timer = 0
onMounted(() => { timer = window.setInterval(() => { activeSlide.value = (activeSlide.value + 1) % heroSlides.length }, 5000) })
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <div class="modules-box" id="home-modules-box">
    <section class="module-item">
      <div class="module-info module-img-text-slideshow-2 w-container-fluid">
        <div class="module-swiper-img-scroll-text-2">
          <div class="scroll-info"><span class="scroll-text">时尚狂欢：全场低至两折！</span><span class="scroll-text">时尚狂欢：全场低至两折！</span><span class="scroll-text">时尚狂欢：全场低至两折！</span></div>
        </div>
        <div class="zeshop-hero-slide">
          <img :src="heroSlides[activeSlide].image" fetchpriority="high" class="img-fluid seo-img" :alt="heroSlides[activeSlide].title">
          <div class="image-text-wrap"><div class="container-fluid content-wrap center"><div class="text-wrap"><div class="sub-title">{{ heroSlides[activeSlide].subtitle }}</div><h2 class="title">{{ heroSlides[activeSlide].title }}</h2><p class="description">{{ heroSlides[activeSlide].description }}</p><a class="btn" @click="navigate(heroSlides[activeSlide].link)">查看详情</a></div></div></div>
        </div>
        <div class="swiper-pagination zeshop-pagination"><span v-for="(_, index) in heroSlides" :key="index" :class="{ active: index === activeSlide }" @click="activeSlide = index"></span></div>
        <button class="zeshop-next" @click="activeSlide = (activeSlide + 1) % heroSlides.length">›</button>
      </div>
    </section>

    <section class="module-item">
      <div class="module-info module-icons">
        <div class="module-title">热门分类 <div class="wave-line"></div></div>
        <div class="module-sub-title">精选商品与时尚风格，探索属于你的独特品味。</div>
        <div class="container-fluid">
          <div class="row g-3 g-lg-4">
            <div v-for="item in categories" :key="item.name" class="col-4 col-lg">
              <a class="text-decoration-none" @click="navigate('/products?category=' + encodeURIComponent(item.match))">
                <div class="image-item d-flex justify-content-center mb-lg-3"><img :src="item.image" class="img-fluid seo-img" :alt="item.name"></div>
                <p class="text-center text-dark mt-2 mb-0 title">{{ item.name }}</p>
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="module-item">
      <div class="module-info module-tab-product">
        <div class="module-title">时尚单品 <div class="wave-line"></div></div>
        <div v-if="keyword" class="module-sub-title">正在浏览“{{ keyword }}”相关商品</div>
        <div class="container-fluid">
          <div class="nav justify-content-center" role="tablist"><a v-for="(tab, index) in tabs" :key="tab.key" class="nav-link" :class="{ active: activeTab === index }" @click="activeTab = index">{{ tab.label }}</a></div>
          <div class="tab-content"><div class="tab-pane fade show active">
            <div class="row g-3 g-lg-4">
              <div v-for="item in tabProducts" :key="item.id" class="col-6 col-md-3"><ProductCard :product="item" /></div>
            </div>
          </div></div>
        </div>
      </div>
    </section>

    <section class="module-item zeshop-luxury-module">
      <div class="module-info module-tab-product">
        <div class="module-title">奢华甄选 <div class="wave-line"></div></div>
        <div class="module-sub-title">奢华品牌、腕表、皮具与鞋履，价格和库存来自真实商品数据。</div>
        <div class="container-fluid">
          <div class="row g-3 g-lg-4">
            <div v-for="item in luxuryProducts" :key="item.id" class="col-6 col-md-3"><ProductCard :product="item" /></div>
          </div>
        </div>
      </div>
    </section>

    <section class="module-item">
      <div class="module-info module-tab-product">
        <div class="module-title">热销推荐 <div class="wave-line"></div></div>
        <div class="container-fluid">
          <div class="row g-3 g-lg-4">
            <div v-for="item in featured" :key="item.id" class="col-6 col-md-3"><ProductCard :product="item" /></div>
          </div>
        </div>
      </div>
    </section>

    <section class="module-item">
      <div class="module-info module-brand">
        <div class="module-title">推荐品牌 <div class="wave-line"></div></div>
        <div class="container-fluid">
          <div class="row g-3 g-lg-4">
            <div v-for="brand in brands" :key="brand.id" class="col-6 col-md-4 col-lg-3">
              <a class="text-decoration-none" @click="navigate('/products?brand=' + encodeURIComponent(brand.name))">
                <div class="brand-item"><img :src="brand.image" :alt="brand.name" class="img-fluid seo-img"></div>
                <p class="text-center text-dark mb-0">{{ brand.name }}</p>
              </a>
            </div>
          </div>
        </div>
        <div class="d-flex justify-content-center mt-4"><a class="btn btn-outline-secondary btn-lg" @click="navigate('/products')">查看全部</a></div>
      </div>
    </section>
  </div>
</template>
