<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/types/user'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<LoginRequest>({
  account: '',
  password: '',
})

const rules: FormRules<LoginRequest> = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const submit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)

  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <p class="auth-card__eyebrow">登录</p>
    <h2>欢迎回来</h2>
    <p>登录后即可上传开发笔记、生成知识点并进入练习闭环。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="账号" prop="account">
        <el-input v-model.trim="form.account" placeholder="用户名或邮箱" size="large" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          placeholder="请输入密码"
          size="large"
          type="password"
          autocomplete="current-password"
          show-password
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-button type="primary" size="large" native-type="submit" :loading="loading">登录</el-button>
    </el-form>
    <RouterLink to="/register">还没有账号？去注册</RouterLink>
  </section>
</template>
