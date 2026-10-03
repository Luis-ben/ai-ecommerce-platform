<script setup lang="ts">
import { computed } from 'vue'
import { navigate } from '../router'
import { cartTotal, formatPrice, isLoggedIn, removeLine, setQuantity, state } from '../store'

const shipping = () => (state.cart.length > 0 && cartTotal.value < 99 ? 10 : 0)
const totalQuantity = computed(() => state.cart.reduce((sum, line) => sum + line.quantity, 0))
function toCheckout(): void { navigate(isLoggedIn.value ? '/checkout' : '/login') }
</script>

<!-- 结构对齐 BeikeShop 主题 cart/cart.blade.php：步骤条 + 左侧商品表 + 右侧合计卡 -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><span class="text-dark">购物车</span></nav></div>
  </div>

  <div class="container" id="app-cart">
    <div class="row mt-1 justify-content-center mb-2">
      <div class="col-12 col-md-9"><div class="steps-wrap">
    <div class="active"><div class="number-wrap"><span class="number">1</span></div><span class="title">购物车</span></div>
    <div><div class="number-wrap"><span class="number">2</span></div><span class="title">确认订单</span></div>
    <div><div class="number-wrap"><span class="number">3</span></div><span class="title">支付</span></div>
  </div></div>
    </div>

    <div v-if="state.cart.length" class="row mt-5">
      <div class="col-12 col-md-9 left-column">
        <div class="card shadow-sm h-min-600">
          <div class="card-body p-lg-4">
            <div class="p-lg-0"><h4 class="mb-3">商品</h4></div>
            <div class="cart-products-wrap table-responsive">
              <table class="table">
                <thead>
                  <tr>
                    <th width="130">商品图</th>
                    <th width="40%">商品</th>
                    <th width="170">数量</th>
                    <th width="170">小计</th>
                    <th width="100" class="text-end">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="line in state.cart" :key="line.productId">
                    <td>
                      <div class="d-flex align-items-center p-image">
                        <input class="form-check-input" type="checkbox" checked>
                        <div class="border d-flex align-items-center justify-content-center wh-80 ms-3"><img :src="line.image" :alt="line.title" class="img-fluid"></div>
                      </div>
                    </td>
                    <td>
                      <a class="name text-truncate-2 mb-1 text-black fw-bold" @click="navigate('/products/' + line.productId)">{{ line.title }}</a>
                      <div class="price text-muted">{{ formatPrice(line.unitPrice) }}</div>
                    </td>
                    <td>
                      <div class="quantity-wrap input-group">
                        <button class="btn quantity-reduce" type="button" @click="setQuantity(line.productId, line.quantity - 1)"><i class="bi bi-dash-lg"></i></button>
                        <input type="text" class="form-control text-center" :value="line.quantity" @change="setQuantity(line.productId, Number(($event.target as HTMLInputElement).value) || 1)">
                        <button class="btn quantity-increase" type="button" @click="setQuantity(line.productId, line.quantity + 1)"><i class="bi bi-plus-lg"></i></button>
                      </div>
                    </td>
                    <td><div class="sub-total">{{ formatPrice(line.subtotal) }}</div></td>
                    <td class="text-end">
                      <button type="button" class="btn text-danger btn-sm px-0" @click="removeLine(line.productId)"><i class="bi bi-x-lg"></i> 删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-md-3 right-column">
        <div class="card shadow-sm x-fixed-top">
          <div class="card-body p-lg-4">
            <div class="total-wrap">
              <div class="p-lg-0"><h4 class="mb-3">商品合计</h4></div>
              <div class="card-body p-lg-0">
                <ul class="list-group list-group-flush">
                  <li class="list-group-item"><span>全部</span><span>{{ state.cart.length }}</span></li>
                  <li class="list-group-item"><span>已选</span><span>{{ totalQuantity }}</span></li>
                  <li class="list-group-item border-bottom-0"><span>商品总计</span><span class="total-price">{{ formatPrice(cartTotal) }}</span></li>
                  <li class="list-group-item border-bottom-0"><span>运费</span><span>{{ shipping() === 0 ? '免运费' : formatPrice(shipping()) }}</span></li>
                  <li class="list-group-item d-grid gap-2 mt-3 border-bottom-0">
                    <button type="button" class="btn btn-primary fs-5 fw-bold" @click="toCheckout">{{ isLoggedIn ? '去结算' : '登录后结算' }}</button>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="d-flex justify-content-center align-items-center flex-column">
      <div class="empty-cart-wrap text-center mt-5">
        <div class="empty-cart-icon mb-3"><i class="bi bi-cart fs-1"></i></div>
        <div class="empty-cart-text mb-3"><h5>购物车是空的</h5><p class="text-muted">快去挑选喜欢的商品吧</p></div>
        <div class="empty-cart-action"><a class="btn btn-primary" @click="navigate('/products')">去购物</a></div>
      </div>
    </div>
  </div>
</template>
