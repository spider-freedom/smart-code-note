<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { ChangePasswordRequest, UpdateUserRequest } from '@/types/user'
import { userApi } from '@/api/user'

const authStore = useAuthStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const profileLoading = ref(false)
const passwordLoading = ref(false)
const avatarUploading = ref(false)

const profileForm = reactive<UpdateUserRequest>({
  nickname: '',
  email: '',
})

const passwordForm = reactive<ChangePasswordRequest & { confirmPassword: string }>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: FormRules<UpdateUserRequest> = {
  nickname: [{ max: 64, message: '昵称不能超过 64 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const passwordRules: FormRules<typeof passwordForm> = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度应为 6-32 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const userInitial = computed(() => {
  const name = authStore.displayName
  return name.slice(0, 1).toUpperCase()
})

const formatJoinDate = (dateStr: string | undefined | null) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日`
}

const avatarUrl = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return avatar
})

watch(
  () => authStore.user,
  (user) => {
    profileForm.nickname = user?.nickname || ''
    profileForm.email = user?.email || ''
  },
  { immediate: true },
)

const handleAvatarUpload = async (options: { file: File }) => {
  avatarUploading.value = true
  try {
    await userApi.uploadAvatar(options.file)
    await authStore.fetchCurrentUser(true)
    ElMessage.success('头像已更新')
  } catch {
    // error handled by interceptor
  } finally {
    avatarUploading.value = false
  }
}

const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const submitProfile = async () => {
  if (!profileFormRef.value) return

  const valid = await profileFormRef.value.validate().catch(() => false)

  if (!valid) return

  profileLoading.value = true
  try {
    await authStore.updateUser({
      nickname: profileForm.nickname || undefined,
      email: profileForm.email || undefined,
    })
    ElMessage.success('个人信息已更新')
  } finally {
    profileLoading.value = false
  }
}

const submitPassword = async () => {
  if (!passwordFormRef.value) return

  const valid = await passwordFormRef.value.validate().catch(() => false)

  if (!valid) return

  passwordLoading.value = true
  try {
    await authStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
    ElMessage.success('密码已修改')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="profile-grid">
    <section class="profile-summary">
      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :http-request="handleAvatarUpload"
          :before-upload="beforeAvatarUpload"
          accept="image/*"
        >
          <el-avatar
            :size="80"
            :src="authStore.user?.avatar ? avatarUrl : undefined"
            class="profile-avatar"
          >
            {{ userInitial }}
          </el-avatar>
          <div class="avatar-overlay">
            <span>更换头像</span>
          </div>
        </el-upload>
        <h2>{{ authStore.displayName }}</h2>
        <p class="username-text">@{{ authStore.user?.username }}</p>
      </div>

      <div class="info-cards">
        <div class="info-card">
          <span class="info-label">用户 ID</span>
          <span class="info-value">{{ authStore.user?.id }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ authStore.user?.email || '未设置' }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">注册时间</span>
          <span class="info-value">{{ formatJoinDate(authStore.user?.createTime) }}</span>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-panel__header">
        <h2>修改资料</h2>
        <p>修改昵称和邮箱，头像点击左侧头像直接上传。</p>
      </div>
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model.trim="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-button type="primary" :loading="profileLoading" @click="submitProfile">保存资料</el-button>
      </el-form>
    </section>

    <section class="form-panel">
      <div class="form-panel__header">
        <h2>修改密码</h2>
        <p>新密码长度需要保持在 6-32 个字符之间。</p>
      </div>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            @keyup.enter="submitPassword"
          />
        </el-form-item>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">修改密码</el-button>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.avatar-section {
  text-align: center;
  margin-bottom: 24px;
}

.avatar-uploader {
  display: inline-block;
  position: relative;
  cursor: pointer;
}

.profile-avatar {
  transition: opacity var(--transition-fast, 0.2s);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.avatar-uploader:hover .avatar-overlay {
  opacity: 1;
}

.avatar-section h2 {
  margin: 12px 0 4px;
  font-size: 20px;
  color: var(--gray-900, #111827);
}

.username-text {
  margin: 0;
  font-size: 14px;
  color: var(--gray-500, #9ca3af);
}

.info-cards {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--gray-50, #f9fafb);
  border-radius: var(--radius-sm, 6px);
}

.info-label {
  font-size: 14px;
  color: var(--gray-500, #6b7280);
}

.info-value {
  font-size: 14px;
  color: var(--gray-700, #374151);
  font-weight: 500;
}
</style>
