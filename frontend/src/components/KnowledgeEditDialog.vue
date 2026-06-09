<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import type { KnowledgeDetail, UpdateKnowledgeRequest } from '@/types/knowledge'

const props = defineProps<{
  knowledge: KnowledgeDetail | null
}>()

const emit = defineEmits<{
  saved: [knowledge: KnowledgeDetail]
  close: []
}>()

const visible = computed(() => props.knowledge !== null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<UpdateKnowledgeRequest>({
  title: '',
  type: '',
  summary: '',
  difficulty: '',
  masteryLevel: 0,
  nextReviewTime: '',
})

const rules: FormRules<UpdateKnowledgeRequest> = {
  title: [
    { required: true, message: '请输入知识点标题', trigger: 'blur' },
    { max: 128, message: '标题不能超过 128 个字符', trigger: 'blur' },
  ],
  type: [{ max: 32, message: '类型不能超过 32 个字符', trigger: 'blur' }],
  difficulty: [{ max: 32, message: '难度不能超过 32 个字符', trigger: 'blur' }],
}

watch(() => props.knowledge, (k) => {
  if (k) {
    form.title = k.title
    form.type = k.type || ''
    form.summary = k.summary || ''
    form.difficulty = k.difficulty || ''
    form.masteryLevel = k.masteryLevel ?? 0
    form.nextReviewTime = k.nextReviewTime || ''
  }
}, { immediate: true })

async function save() {
  if (!formRef.value || !props.knowledge) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const result = await knowledgeApi.update(props.knowledge.id, {
      title: form.title,
      type: form.type || undefined,
      summary: form.summary || undefined,
      difficulty: form.difficulty || undefined,
      masteryLevel: form.masteryLevel,
      nextReviewTime: form.nextReviewTime || undefined,
    })
    ElMessage.success('知识点已更新')
    emit('saved', result)
    emit('close')
  } finally {
    saving.value = false
  }
}

function cancel() {
  emit('close')
}
</script>

<template>
  <el-dialog v-model="visible" title="编辑知识点" width="680px" @close="cancel">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="标题" prop="title">
        <el-input v-model.trim="form.title" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-input v-model.trim="form.type" />
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="form.summary" type="textarea" :rows="5" />
      </el-form-item>
      <el-form-item label="难度" prop="difficulty">
        <el-input v-model.trim="form.difficulty" />
      </el-form-item>
      <el-form-item label="掌握程度" prop="masteryLevel">
        <el-slider v-model="form.masteryLevel" :min="0" :max="5" show-stops />
      </el-form-item>
      <el-form-item label="下次复习时间" prop="nextReviewTime">
        <el-date-picker
          v-model="form.nextReviewTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择下次复习时间"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="cancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>
