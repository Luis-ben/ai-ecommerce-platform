<script setup lang="ts">
import { computed, ref } from 'vue'
import { navigate } from '../router'
import { cartTotal, checkout, formatPrice, isLoggedIn, state } from '../store'

const comment = ref('')
const shipping = () => (state.cart.length > 0 && cartTotal.value < 99 ? 10 : 0)
const payable = computed(() => cartTotal.value + shipping())
const totalQuantity = computed(() => state.cart.reduce((sum, line) => sum + line.quantity, 0))

async function submit(): Promise<void> {
  if (!isLoggedIn.value) { navigate('/login'); return }
  const order = await checkout()
  if (order) navigate('/orders/' + order.id)
}
</script>

<!-- 结构对齐 BeikeShop 主题 checkout.blade.php：步骤条 + 左卡 + 右侧合计卡 + #submit-checkout -->
<template>
  <div class="breadcrumb-filter">
    <div class="container-fluid"><nav class="breadcrumb"><a @click="navigate('/')">首页</a><span class="px-2">/</span><a @click="navigate('/cart')">购物车</a><span class="px-2">/</span><span class="text-dark">确认订单</span></nav></div>
  </div>

  <div class="container">
    <div class="row mt-1 justify-content-center">
      <div class="col-12 col-md-9"><div class="steps-wrap">
    <div class="active"><div class="number-wrap"><span class="number">1</span></div><span class="title">购物车</span></div>
    <div class="active"><div class="number-wrap"><span class="number">2</span></div><span class="title">确认订单</span></div>
    <div><div class="number-wrap"><span class="number">3</span></div><span class="title">支付</span></div>
  </div></div>
    </div>

    <div class="row mt-5">
      <div class="col-12 col-md-8 left-column">
        <div class="card shadow-sm">
          <div class="card-body p-lg-4">
            <div class="checkout-black">
              <h5 class="checkout-title">收货信息</h5>
              <p class="text-muted mb-0">演示后端以下单时的服务端购物车生成订单，暂不保存收货地址与收货人；下单账号为 {{ state.user?.display_name }}。</p>
            </div>
            <div class="checkout-black">
              <h5 class="checkout-title">支付方式</h5>
              <div class="radio-line-wrap" id="payment-methods-wrap">
                <div class="radio-line-item active">
                  <div class="left"><span class="radio"></span></div>
                  <div class="right ms-2"><div class="title">模拟支付（SIMULATED）</div></div>
                </div>
              </div>
            </div>
            <div class="checkout-black">
              <h5 class="checkout-title">订单备注</h5>
              <div class="comment-wrap" id="comment-wrap"><textarea v-model="comment" rows="5" class="form-control" placeholder="订单备注（选填）"></textarea></div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-md-4 right-column">
        <div class="x-fixed-top">
          <div class="card total-wrap p-lg-4 shadow-sm">
            <div class="card-header d-flex align-items-center justify-content-between">
              <h5 class="mb-0">购物车合计</h5><span class="rounded-circle bg-primary">{{ totalQuantity }}</span>
            </div>
            <div class="card-body">
              <div class="products-wrap">
                <div v-for="line in state.cart" :key="line.productId" class="item">
                  <div class="image">
                    <div class="img border d-flex align-items-center justify-content-center wh-50 me-2"><img :src="line.image" class="img-fluid" :alt="line.title"></div>
                    <div class="name"><div class="text-truncate-2" :title="line.title">{{ line.title }}</div></div>
                  </div>
                  <div class="price text-end"><div>{{ formatPrice(line.subtotal) }}</div><div class="quantity">x {{ line.quantity }}</div></div>
                </div>
              </div>
              <ul class="totals">
                <li><span>商品总计</span><span>{{ formatPrice(cartTotal) }}</span></li>
                <li><span>运费</span><span>{{ shipping() === 0 ? '免运费' : formatPrice(shipping()) }}</span></li>
                <li><span>应付</span><span>{{ formatPrice(payable) }}</span></li>
              </ul>
              <div class="d-grid gap-2 mt-3 submit-checkout-wrap">
                <button class="btn btn-primary fw-bold fs-5" type="button" id="submit-checkout" :disabled="state.busy || !state.cart.length" @click="submit">{{ isLoggedIn ? '提交订单' : '登录后提交' }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
