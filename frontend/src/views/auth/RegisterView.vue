<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { RegisterRequest } from '@/types/user'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<RegisterRequest>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: '',
})

const rules: FormRules<RegisterRequest> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度应为 3-32 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  nickname: [{ max: 64, message: '昵称不能超过 64 个字符', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度应为 6-32 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const submit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)

  if (!valid) return

  loading.value = true
  try {
    await authStore.register({
      ...form,
      email: form.email || undefined,
      nickname: form.nickname || undefined,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <p class="auth-card__eyebrow">注册</p>
    <h2>创建学习账号</h2>
    <p>创建账号后，你的笔记、练习记录和复习计划会按用户隔离保存。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" placeholder="3-32 个字符" size="large" autocomplete="username" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model.trim="form.nickname" placeholder="可选" size="large" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model.trim="form.email" placeholder="可选" size="large" autocomplete="email" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          placeholder="6-32 个字符"
          size="large"
          type="password"
          autocomplete="new-password"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          placeholder="再次输入密码"
          size="large"
          type="password"
          autocomplete="new-password"
          show-password
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-button type="primary" size="large" native-type="submit" :loading="loading">注册</el-button>
    </el-form>
    <RouterLink to="/login">已有账号？去登录</RouterLink>
  </section>
</template>
