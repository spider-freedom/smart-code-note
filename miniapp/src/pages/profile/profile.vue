<template>
  <view class="page-container">
    <!-- 头像 + 信息卡片 -->
    <view class="profile-card card">
      <view class="avatar-area" @tap="chooseAvatar">
        <image v-if="userInfo?.avatar" class="avatar-img" :src="avatarUrl" mode="aspectFill" />
        <text v-else class="avatar-placeholder">{{ initialChar }}</text>
        <view class="avatar-camera">📷</view>
      </view>

      <text class="nickname">{{ userInfo?.nickname || '微信用户' }}</text>
      <text class="username">@{{ userInfo?.username }}</text>

      <view class="join-time">
        <text class="join-label">加入于</text>
        <text class="join-date">{{ formatJoinDate(userInfo?.createTime) }}</text>
      </view>
    </view>

    <!-- 学习统计 -->
    <view class="stats-row">
      <view class="stat-card">
        <text class="stat-value">{{ overview?.totalNotes || 0 }}</text>
        <text class="stat-label">笔记</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ overview?.totalKnowledgePoints || 0 }}</text>
        <text class="stat-label">知识点</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ overview?.masteredKnowledgePoints || 0 }}</text>
        <text class="stat-label">已掌握</text>
      </view>
    </view>

    <!-- 个人信息编辑 -->
    <view class="card edit-section">
      <view class="section-header">
        <text class="section-title">个人信息</text>
        <text class="edit-toggle" @tap="toggleEdit">{{ editing ? '完成' : '编辑' }}</text>
      </view>

      <view v-if="editing" class="edit-form">
        <view class="form-item">
          <text class="form-label">昵称</text>
          <input class="form-input" v-model="editForm.nickname" placeholder="请输入昵称" maxlength="32" />
        </view>
        <view class="form-item">
          <text class="form-label">邮箱</text>
          <input class="form-input" v-model="editForm.email" placeholder="请输入邮箱" type="text" />
        </view>
        <view class="form-item">
          <text class="form-label">用户名</text>
          <text class="form-readonly">{{ userInfo?.username }}</text>
        </view>
        <button class="save-btn" @tap="handleSave" :loading="saving">保存</button>
      </view>

      <view v-else class="info-list">
        <view class="info-row">
          <text class="info-label">昵称</text>
          <text class="info-value">{{ userInfo?.nickname || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">邮箱</text>
          <text class="info-value">{{ userInfo?.email || '未设置' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">用户名</text>
          <text class="info-value">{{ userInfo?.username }}</text>
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="menu-section">
      <view class="menu-item" @tap="goToPage('/pages/practice/practice')">
        <text>在线练习</text>
        <text class="menu-arrow">→</text>
      </view>
      <view class="menu-item" @tap="goToPage('/pages/reviews/reviews')">
        <text>复习计划</text>
        <text class="menu-arrow">→</text>
      </view>
      <view class="menu-item" @tap="goToPage('/pages/questions/list/list')">
        <text>错题本</text>
        <text class="menu-arrow">→</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'
import { reportApi } from '@/api/report'

const authStore = useAuthStore()
const userInfo = ref(null)
const overview = ref(null)
const editing = ref(false)
const saving = ref(false)

const editForm = ref({ nickname: '', email: '' })

const initialChar = computed(() => (userInfo.value?.nickname || '微')[0])

const avatarUrl = computed(() => {
  const avatar = userInfo.value?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `http://localhost:8080${avatar}`
})

onMounted(async () => {
  try {
    const [user, ov] = await Promise.all([
      userApi.getCurrentUser(),
      reportApi.getOverview().catch(() => null)
    ])
    userInfo.value = user
    overview.value = ov
    editForm.value.nickname = user.nickname || ''
    editForm.value.email = user.email || ''
  } catch (e) { /* ignore */ }
})

function formatJoinDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日`
}

function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      uni.showLoading({ title: '上传中...' })
      try {
        const updated = await userApi.uploadAvatar(filePath)
        userInfo.value = updated
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

function toggleEdit() {
  if (editing.value) {
    editing.value = false
    editForm.value.nickname = userInfo.value?.nickname || ''
    editForm.value.email = userInfo.value?.email || ''
  } else {
    editing.value = true
  }
}

async function handleSave() {
  saving.value = true
  try {
    const updated = await userApi.updateUser({
      nickname: editForm.value.nickname,
      email: editForm.value.email || undefined
    })
    userInfo.value = updated
    editing.value = false
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) { /* ignore */ } finally {
    saving.value = false
  }
}

function goToPage(url) {
  uni.navigateTo({ url })
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) authStore.logout()
    }
  })
}
</script>

<style scoped>
.profile-card {
  text-align: center;
  padding: 48rpx 32rpx 32rpx;
}

.avatar-area {
  position: relative;
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  margin: 0 auto 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #dbeafe, #eef2ff);
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-placeholder {
  font-size: 56rpx;
  font-weight: 700;
  color: #1d4ed8;
}

.avatar-camera {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #1d4ed8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  border: 3rpx solid #fff;
}

.nickname {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
  display: block;
  margin-bottom: 6rpx;
}

.username {
  font-size: 26rpx;
  color: #9ca3af;
  display: block;
}

.join-time {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f3f4f6;
}

.join-label {
  font-size: 24rpx;
  color: #9ca3af;
}

.join-date {
  font-size: 24rpx;
  color: #6b7280;
  font-weight: 500;
}

.stats-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.stat-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #1d4ed8;
  display: block;
  margin-bottom: 4rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #6b7280;
}

.edit-section {
  padding-bottom: 32rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.edit-toggle {
  font-size: 26rpx;
  color: #2563eb;
  padding: 4rpx 16rpx;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.form-label {
  font-size: 26rpx;
  color: #6b7280;
}

.form-input {
  height: 80rpx;
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #374151;
  border: 1rpx solid #e5e7eb;
}

.form-readonly {
  font-size: 28rpx;
  color: #9ca3af;
  padding: 16rpx 24rpx;
  background: #f9fafb;
  border-radius: 12rpx;
}

.save-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #1d4ed8;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 28rpx;
  color: #6b7280;
}

.info-value {
  font-size: 28rpx;
  color: #374151;
}

.menu-section {
  margin-top: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 24rpx;
  font-size: 28rpx;
  color: #374151;
  border-bottom: 1rpx solid #f3f4f6;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-arrow {
  color: #d1d5db;
}

.logout-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #fef2f2;
  color: #ef4444;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
  margin-top: 32rpx;
}
</style>
