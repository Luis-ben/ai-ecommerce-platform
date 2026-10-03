<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AiAssistant from './components/AiAssistant.vue'
import HomePage from './pages/HomePage.vue'
import CatalogPage from './pages/CatalogPage.vue'
import ProductDetailPage from './pages/ProductDetailPage.vue'
import CartPage from './pages/CartPage.vue'
import CheckoutPage from './pages/CheckoutPage.vue'
import OrdersPage from './pages/OrdersPage.vue'
import OrderDetailPage from './pages/OrderDetailPage.vue'
import FavoritesPage from './pages/FavoritesPage.vue'
import TicketsPage from './pages/TicketsPage.vue'
import LoginPage from './pages/LoginPage.vue'
import AccountPage from './pages/AccountPage.vue'
import AdminDashboard from './admin/AdminDashboard.vue'
import AdminDataModule from './admin/AdminDataModule.vue'
import AdminReports from './admin/AdminReports.vue'
import AdminAi from './admin/AdminAi.vue'
import AdminOrders from './admin/AdminOrders.vue'
import AdminCustomers from './admin/AdminCustomers.vue'
import AdminArticles from './admin/AdminArticles.vue'
import AdminSystem from './admin/AdminSystem.vue'
import AdminHelp from './admin/AdminHelp.vue'
import AdminProducts from './admin/AdminProducts.vue'
import AdminCatalogTerms from './admin/AdminCatalogTerms.vue'
import AdminProductTrash from './admin/AdminProductTrash.vue'
import AdminOrderTrash from './admin/AdminOrderTrash.vue'
import AdminTickets from './admin/AdminTickets.vue'
import AdminTicketDetail from './admin/AdminTicketDetail.vue'
import AdminConversations from './admin/AdminConversations.vue'
import AdminAiMonitor from './admin/AdminAiMonitor.vue'
import { navigate, page } from './router'
import { cartCount, isLoggedIn, isStaff, loadProducts, logout, refreshCart, state } from './store'

const keyword = ref('')
const searchOpen = ref(false)
const mobileMenuOpen = ref(false)
const userMenuOpen = ref(false)

const q = (value: string) => encodeURIComponent(value)
/** 导航项全部落到真实筛选参数，避免出现点进去没有结果的死链。 */
const navItems = [
  { name: '运动户外', badge: '', badgeBg: '', full: true, to: () => '/products?category=' + q('运动户外'), groups: [
    { title: '引领时尚', links: ['运动户外', '鞋履', '配饰'].map((n) => ({ label: n, to: () => '/products?category=' + q(n) })) },
    { title: '热门品牌', links: ['Gucci', 'Prada', 'Dior', 'Chanel'].map((n) => ({ label: n, to: () => '/products?brand=' + q(n) })) },
    { title: '爆款商品', image: '/image/catalog/demo/banner/2_en.jpg' },
  ] },
  { name: '时尚服饰', badge: '新品', badgeBg: '#7c3aed', full: true, to: () => '/products?category=' + q('女装'), groups: [
    { title: '分类', links: ['女装', '男装', '上装', '下装'].map((n) => ({ label: n, to: () => '/products?category=' + q(n) })) },
    { title: '热门品牌', links: ['Chanel', 'Balenciaga', 'Valentino'].map((n) => ({ label: n, to: () => '/products?brand=' + q(n) })) },
  ] },
  { name: '清仓特卖', badge: '', badgeBg: '', full: false, to: () => '/products?sort=price-asc&max_price=300', groups: [] },
  { name: '热销榜', badge: '热卖', badgeBg: '#fd560f', full: false, to: () => '/products?sort=sales-desc', groups: [] },
  { name: '品牌馆', badge: '大牌直销', badgeBg: '#0f9b8e', full: true, to: () => '/products', groups: [
    { title: '热门品牌', links: ['Gucci', 'Prada', 'Dior', 'Chanel', 'Louis Vuitton'].map((n) => ({ label: n, to: () => '/products?brand=' + q(n) })) },
    { title: '奢华甄选', links: ['Hermès', 'Rolex', 'Cartier', 'Fendi'].map((n) => ({ label: n, to: () => '/products?brand=' + q(n) })) },
  ] },
  { name: '全部商品', badge: '', badgeBg: '', full: false, to: () => '/products', groups: [] },
]
const hotTags = ['跑鞋', '连衣裙', '双肩包', '腕表', 'Gucci', 'Rolex']

function go(path: string): void {
  searchOpen.value = false
  mobileMenuOpen.value = false
  userMenuOpen.value = false
  navigate(path)
}

function search(): void {
  const value = keyword.value.trim()
  go('/products' + (value ? '?keyword=' + q(value) : ''))
}

function searchByTag(tag: string): void {
  keyword.value = tag
  search()
}

async function doLogout(): Promise<void> {
  userMenuOpen.value = false
  await logout()
  navigate('/')
}

onMounted(async () => {
  await loadProducts()
  if (isLoggedIn.value) await refreshCart()
})
</script>

<template>
  <AdminOrderTrash v-if="page.name === 'admin-order-trash'" />
  <AdminProductTrash v-else-if="page.name === 'admin-product-trash'" />
  <AdminCatalogTerms v-else-if="page.name === 'admin-catalog-terms'" :key="page.param" :kind="page.param" :title="page.param === 'rma-reasons' ? '售后原因' : page.param === 'attribute-groups' ? '属性组' : page.param === 'attributes' ? '属性' : page.param === 'categories' ? '商品分类' : page.param === 'brands' ? '商品品牌' : '高级筛选'" />
  <AdminProducts v-else-if="page.name === 'admin-products'" />
  <AdminTickets v-else-if="page.name === 'admin-tickets'" />
  <AdminTicketDetail v-else-if="page.name === 'admin-ticket-detail'" :key="page.param" :id="page.param" />
  <AdminConversations v-else-if="page.name === 'admin-conversations'" />
  <AdminDashboard v-else-if="page.name === 'admin-dashboard'" />
  <AdminReports v-else-if="page.name === 'admin-reports'" />
  <AdminAi v-else-if="page.name === 'admin-ai'" />
  <AdminAiMonitor v-else-if="page.name === 'admin-ai-monitor'" />
  <AdminOrders v-else-if="page.name === 'admin-orders'" />
  <AdminCustomers v-else-if="page.name === 'admin-customers'" />
  <AdminArticles v-else-if="page.name === 'admin-articles'" />
  <AdminSystem v-else-if="page.name === 'admin-system'" />
  <AdminHelp v-else-if="page.name === 'admin-help'" />
  <AdminDataModule v-else-if="page.name === 'admin-placeholder'" :key="page.param" :path="page.param" />
  <div v-else class="zs-app">
    <header>
      <div class="top-wrap d-none d-lg-block">
        <div class="container-fluid d-flex justify-content-between align-items-center">
          <div class="left d-flex align-items-center gap-3">
            <span class="top-currency">CNY ¥</span>
            <span class="top-sep">|</span>
            <span>简体中文</span>
            <span class="top-sep">|</span>
            <a v-if="state.user" @click="go('/account')">{{ state.user.display_name }}</a>
            <template v-else>
              <a @click="go('/login')">登录</a>
              <span class="top-sep">|</span>
              <a @click="go('/login')">注册</a>
            </template>
          </div>
          <div class="right nav d-flex align-items-center gap-3">
            <a v-if="state.user" @click="doLogout">退出</a>
            <div class="my-auto"><i class="bi bi-telephone-forward me-2"></i> 客服热线 028-8888-6666</div>
          </div>
        </div>
      </div>

      <div class="header-content d-none d-lg-block">
        <div class="container-fluid navbar-expand-lg">
          <div class="logo zeshop-logo"><a @click="go('/')"><span class="zeshop-logo-mark">Z</span><span class="zeshop-logo-type"><strong>Zeshop</strong><small>SHOP SMARTER</small></span></a></div>
          <div class="menu-wrap">
            <ul class="navbar-nav">
              <li v-for="item in navItems" :key="item.name" class="nav-item" :class="{ dropdown: item.groups.length }">
                <a class="nav-link fw-bold" :class="{ 'dropdown-toggle': item.groups.length }" @click="go(item.to())">
                  {{ item.name }}
                  <span v-if="item.badge" class="badge" :style="{ backgroundColor: item.badgeBg, color: '#fff', borderColor: item.badgeBg }">{{ item.badge }}</span>
                  <svg v-if="item.groups.length" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg"><path d="m6 9 6 6 6-6"/></svg>
                </a>
                <div v-if="item.groups.length" class="dropdown-menu">
                  <div class="card card-lg"><div class="card-body"><div class="container"><div class="row">
                    <div v-for="group in item.groups" :key="group.title" class="col-6 col-md">
                      <div v-if="group.title" class="mb-3 fw-bold group-name">{{ group.title }}</div>
                      <a v-if="group.image" @click="go('/products')"><img :src="group.image" :alt="group.title" class="img-fluid"></a>
                      <ul v-else class="nav flex-column ul-children">
                        <li v-for="link in group.links" :key="link.label" class="nav-item"><a class="nav-link px-0" @click="go(link.to())">{{ link.label }}</a></li>
                      </ul>
                    </div>
                  </div></div></div></div>
                </div>
              </li>
            </ul>
          </div>
          <div class="zeshop-search-center">
            <div class="zeshop-inline-search">
              <i class="bi bi-search"></i>
              <input v-model="keyword" placeholder="搜索商品" @keyup.enter="search">
              <button type="button" @click="search">搜索</button>
            </div>
          </div>
          <div class="right-btn">
            <ul class="navbar-nav flex-row">
              <li class="nav-item"><a class="nav-link" title="搜索" @click="searchOpen = true"><img src="/image/icons/search.svg" class="img-fluid" alt="搜索"></a></li>
              <li class="nav-item"><a class="nav-link" title="我的收藏" @click="go('/favorites')"><img src="/image/icons/favorite.svg" class="img-fluid" alt="我的收藏"></a></li>
              <li class="nav-item"><a class="nav-link" title="我的" @click="state.user ? (userMenuOpen = !userMenuOpen) : go('/login')"><img src="/image/icons/account.svg" class="img-fluid" alt="我的"></a></li>
              <li class="nav-item"><a class="nav-link position-relative" title="购物车" @click="go('/cart')"><img src="/image/icons/cart.svg" class="img-fluid" alt="购物车"><span v-if="cartCount > 0" class="cart-badge-quantity" style="display:block">{{ cartCount }}</span></a></li>
            </ul>
            <div v-if="userMenuOpen && state.user" class="zs-dropdown" @click="userMenuOpen = false">
              <button class="zs-dropdown-item" type="button" @click="go('/account')">个人中心</button>
              <button class="zs-dropdown-item" type="button" @click="go('/orders')">我的订单</button>
              <button class="zs-dropdown-item" type="button" @click="go('/tickets')">我的工单</button>
              <button v-if="isStaff" class="zs-dropdown-item" type="button" @click="go('/admin/tickets')">客服工作台</button>
              <button v-if="isStaff" class="zs-dropdown-item" type="button" @click="go('/admin/conversations')">聊天记录</button>
              <button v-if="isStaff" class="zs-dropdown-item" type="button" @click="go('/admin/products')">商品管理</button>
              <button class="zs-dropdown-item danger" type="button" @click="doLogout">退出登录</button>
            </div>
          </div>
        </div>
      </div>

      <div class="header-mobile d-lg-none">
        <div class="mobile-content">
          <div class="left">
            <div class="mobile-open-menu" @click="mobileMenuOpen = true"><img src="/image/icons/menu.svg" alt="菜单" class="img-fluid"></div>
            <div class="mobile-open-search" @click="searchOpen = true"><img src="/image/icons/search.svg" class="img-fluid" alt="搜索"></div>
          </div>
          <div class="center" @click="go('/')"><span class="zeshop-logo-mark">Z</span><span class="zeshop-logo-type"><strong>Zeshop</strong><small>SHOP SMARTER</small></span></div>
          <div class="right">
            <a class="nav-link mb-account-icon" @click="state.user ? go('/account') : go('/login')"><img src="/image/icons/account.svg" class="img-fluid" alt="我的"></a>
            <a class="nav-link ms-3 m-cart position-relative" @click="go('/cart')"><img src="/image/icons/cart.svg" alt="购物车" class="img-fluid"><span v-if="cartCount > 0" class="cart-badge-quantity" style="display:block">{{ cartCount }}</span></a>
          </div>
        </div>
      </div>

      <div v-if="mobileMenuOpen" class="zs-backdrop" @click="mobileMenuOpen = false"></div>
      <aside class="zs-offcanvas" :class="{ open: mobileMenuOpen }">
        <div class="zs-offcanvas-header"><span class="fw-bold">菜单</span><button class="zs-mobile-btn" type="button" @click="mobileMenuOpen = false">✕</button></div>
        <div class="zs-offcanvas-body">
          <a class="zs-drawer-item" @click="go('/')">首页</a>
          <a v-for="item in navItems" :key="item.name" class="zs-drawer-item" @click="go(item.to())">{{ item.name }}</a>
          <a class="zs-drawer-item" @click="go('/products')">全部商品</a>
          <a v-if="state.user" class="zs-drawer-item" @click="go('/orders')">我的订单</a>
          <a v-if="state.user" class="zs-drawer-item" @click="go('/account')">个人中心</a>
          <a v-if="isStaff" class="zs-drawer-item" @click="go('/admin/tickets')">客服工作台</a>
          <a v-if="!state.user" class="zs-drawer-item" @click="go('/login')">登录 / 注册</a>
        </div>
      </aside>

      <div v-if="searchOpen" class="zeshop-search">
        <div class="container">
          <div class="d-flex align-items-center gap-3 py-4">
            <div class="input-group">
              <input v-model="keyword" class="form-control form-control-lg fs-5" placeholder="请输入搜索内容" @keyup.enter="search">
              <span class="input-group-text" @click="search"><i class="bi bi-search"></i></span>
            </div>
            <button type="button" class="btn-close" aria-label="关闭" @click="searchOpen = false"></button>
          </div>
          <div class="mb-4">
            <h5>热门搜索</h5>
            <div class="d-flex flex-wrap gap-2">
              <button v-for="tag in hotTags" :key="tag" class="zs-hot-tag" type="button" @click="searchByTag(tag)">{{ tag }}</button>
            </div>
          </div>
          <h5>热门商品</h5>
          <div class="row g-3 g-lg-4">
            <div v-for="item in state.products.slice(0, 4)" :key="item.id" class="col-6 col-md-3">
              <div class="product-wrap">
                <div class="image" @click="go('/products/' + item.id)"><img :src="item.image || ''" class="img-fluid image-before" :alt="item.title"></div>
                <div class="product-bottom-info"><div class="product-name" @click="go('/products/' + item.id)">{{ item.title }}</div><div class="product-price"><span class="ze-price">{{ item.price.toFixed(2) }}</span></div></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="zs-main">
      <HomePage v-if="page.name === 'home'" />
      <CatalogPage v-else-if="page.name === 'catalog'" />
      <ProductDetailPage v-else-if="page.name === 'product'" :key="page.param" :id="page.param" />
      <CartPage v-else-if="page.name === 'cart'" />
      <CheckoutPage v-else-if="page.name === 'checkout'" />
      <OrdersPage v-else-if="page.name === 'orders'" />
      <OrderDetailPage v-else-if="page.name === 'order-detail'" :key="page.param" :id="page.param" />
      <FavoritesPage v-else-if="page.name === 'favorites'" />
      <TicketsPage v-else-if="page.name === 'tickets'" />
      <LoginPage v-else-if="page.name === 'login'" />
      <AccountPage v-else-if="page.name === 'account'" />
      <div v-else class="ze-page text-center"><h1 class="ze-page-title">页面不存在</h1><p class="text-secondary">你访问的地址没有对应页面。</p><button class="btn btn-dark" @click="go('/')">返回首页</button></div>
    </main>

    <footer class="zs-footer">
      <div class="container"><div class="services-wrap"><div class="row align-items-lg-center">
        <div v-for="service in [{ title: '优惠活动', sub: '满500元立减90元', icon: '2.png' }, { title: '精致服务', sub: '完善的售后保障', icon: '1.png' }, { title: '全球配送', sub: '多仓快速发货', icon: '4.png' }, { title: '无忧退货', sub: '放心购物退货无忧', icon: '3.png' }]" :key="service.title" class="col-lg-3 col-md-6 col-6">
          <div class="service-item my-1"><div class="icon"><img :src="'/image/catalog/demo/services-icon/' + service.icon" class="img-fluid" :alt="service.title"></div><div class="text"><p class="title">{{ service.title }}</p><p class="sub-title">{{ service.sub }}</p></div></div>
        </div>
      </div></div></div>
      <div class="container"><div class="footer-content"><div class="row">
        <div class="col-12 col-md-3 me-lg-5">
          <div class="footer-content-left footer-link-wrap">
            <h6 class="text-uppercase intro-title">公司简介</h6>
            <div class="intro-wrap">
              <div class="logo zeshop-logo" @click="go('/')"><span class="zeshop-logo-mark">Z</span><span class="zeshop-logo-type"><strong>Zeshop</strong><small>SHOP SMARTER</small></span></div>
              <div class="text">Zeshop 在线商城，商品价格、库存与订单状态均来自数据库实时查询。</div>
              <div class="social-network"><a v-for="icon in ['twitter', 'facebook', 'youtube', 'instagram', 'pinterest']" :key="icon"><img :src="'/image/catalog/demo/social/' + icon + '.png'" class="img-fluid" :alt="icon"></a></div>
            </div>
          </div>
        </div>
        <div v-for="section in [{ title: '关于我们', links: ['关于我们', '配送信息', '隐私政策', '联系客服'] }, { title: '我的账户', links: ['个人中心', '我的订单', '我的工单', '购物车'] }, { title: '购物指南', links: ['全部商品', '清仓特卖', '品牌馆', '使用条款'] }]" :key="section.title" class="col-12 col-md footer-content-link1 footer-link-wrap">
          <h6 class="text-uppercase">{{ section.title }}</h6>
          <ul class="list-unstyled"><li v-for="link in section.links" :key="link" class="lh-lg"><a @click="go('/products')">{{ link }}</a></li></ul>
        </div>
        <div class="col-12 col-md-3 footer-content-contact footer-link-wrap">
          <h6 class="text-uppercase">联系我们</h6>
          <ul class="list-unstyled">
            <li class="lh-lg"><i class="bi bi-envelope-fill"></i> support@zeshop.local</li>
            <li class="lh-lg"><i class="bi bi-telephone-fill"></i> 028-8888-6666</li>
            <li class="lh-lg"><i class="bi bi-geo-alt-fill"></i> 09:00 - 21:00</li>
          </ul>
        </div>
      </div></div></div>
      <div class="footer-bottom"><div class="container"><div class="d-lg-flex align-items-center justify-content-center"><div class="copyright-content">© 2026 Zeshop · 界面参考 BeikeShop 开源商城（OSL-3.0），未复制其源码与素材</div><div class="ms-auto right-img py-md-2 text-center"><img src="/image/catalog/demo/banner/pay_icons.png" class="img-fluid" alt="支付方式"></div></div></div></div>
    </footer>

    <AiAssistant />
  </div>
</template>
