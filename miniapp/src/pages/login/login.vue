<template>
  <view class="login-page">
    <view class="login-card">
      <view class="logo-area">
        <text class="logo-icon">📘</text>
        <text class="app-name">智能开发笔记</text>
        <text class="app-desc">AI 驱动的编程学习伴侣</text>
      </view>

      <button class="wx-login-btn" @tap="handleWxLogin" :loading="loading">
        <text class="wx-icon">🟢</text>
        <text>微信一键登录</text>
      </button>

      <text class="privacy-tip">登录即表示同意《用户协议》和《隐私政策》</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'

const authStore = useAuthStore()
const loading = ref(false)

async function handleWxLogin() {
  loading.value = true
  try {
    const loginRes = await wxLogin()
    if (!loginRes || !loginRes.code) {
      uni.showToast({ title: '获取微信授权失败', icon: 'none' })
      return
    }
    const result = await userApi.wxLogin(loginRes.code)
    authStore.setAuth(result.token, result.user)
    uni.switchTab({ url: '/pages/index/index' })
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function wxLogin() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: resolve,
      fail: reject
    })
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff 0%, #dbeafe 100%);
  padding: 40rpx;
}

.login-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 80rpx 48rpx 60rpx;
  width: 100%;
  max-width: 600rpx;
  box-shadow: 0 8rpx 40rpx rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo-area {
  text-align: center;
  margin-bottom: 64rpx;
}

.logo-icon {
  font-size: 96rpx;
  display: block;
  margin-bottom: 24rpx;
}

.app-name {
  font-size: 40rpx;
  font-weight: 700;
  color: #1d4ed8;
  display: block;
  margin-bottom: 12rpx;
}

.app-desc {
  font-size: 26rpx;
  color: #6b7280;
}

.wx-login-btn {
  width: 100%;
  height: 96rpx;
  background: #07c160;
  color: #fff;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  margin-bottom: 32rpx;
}

.wx-login-btn::after {
  border: none;
}

.wx-icon {
  margin-right: 12rpx;
  font-size: 36rpx;
}

.privacy-tip {
  font-size: 22rpx;
  color: #9ca3af;
  text-align: center;
}
</style>
