<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Edit, Loading, MagicStick } from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/knowledge'
import { questionApi } from '@/api/question'
import DifficultyTag from '@/components/DifficultyTag.vue'
import KnowledgeEditDialog from '@/components/KnowledgeEditDialog.vue'
import KnowledgeLevelTag from '@/components/KnowledgeLevelTag.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import type { KnowledgeDetail } from '@/types/knowledge'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const deleting = ref(false)
const generating = ref(false)
const aiStreamText = ref('')
const showStreamDialog = ref(false)
const editingKnowledge = ref<KnowledgeDetail | null>(null)
const knowledge = ref<KnowledgeDetail | null>(null)
let abortController: AbortController | null = null

const knowledgeId = computed(() => route.params.id as string)

const loadDetail = async () => {
  loading.value = true
  try {
    knowledge.value = await knowledgeApi.detail(knowledgeId.value)
  } finally {
    loading.value = false
  }
}

const openEdit = () => {
  editingKnowledge.value = knowledge.value
}

const onEditSaved = (updated: KnowledgeDetail) => {
  knowledge.value = updated
  editingKnowledge.value = null
}

const deleteKnowledge = async () => {
  if (!knowledge.value) return

  await ElMessageBox.confirm(`确定删除知识点「${knowledge.value.title}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })

  deleting.value = true
  try {
    await knowledgeApi.delete(knowledgeId.value)
    ElMessage.success('知识点已删除')
    router.push('/knowledge')
  } finally {
    deleting.value = false
  }
}

const generateQuestions = () => {
  if (!knowledge.value) return

  generating.value = true
  aiStreamText.value = ''
  showStreamDialog.value = true

  abortController = questionApi.generateStream(
    { knowledgeId: knowledge.value.id, count: 3 },
    (chunk) => {
      aiStreamText.value += chunk
    },
    (result) => {
      generating.value = false
      showStreamDialog.value = false
      ElMessage.success(`已生成 ${result.length} 道题目`)
      router.push(`/questions?knowledgeId=${knowledge.value!.id}`)
    },
    (error) => {
      generating.value = false
      ElMessage.error(error)
    },
  )
}

const cancelGeneration = () => {
  abortController?.abort()
  generating.value = false
  showStreamDialog.value = false
}

onMounted(loadDetail)

onUnmounted(() => {
  abortController?.abort()
})
</script>

<template>
  <section class="detail-page" v-loading="loading">
    <div class="detail-page__header">
      <el-button :icon="ArrowLeft" @click="router.push('/knowledge')">返回列表</el-button>
      <div class="detail-page__actions">
        <el-button :icon="Edit" @click="openEdit">编辑</el-button>
        <el-button :icon="MagicStick" :loading="generating" @click="generateQuestions">
          生成题目
        </el-button>
        <el-button type="danger" :icon="Delete" :loading="deleting" @click="deleteKnowledge">删除</el-button>
      </div>
    </div>

    <el-empty v-if="!knowledge && !loading" description="知识点不存在" />

    <template v-if="knowledge">
      <article class="detail-title-card">
        <div>
          <h2>{{ knowledge.title }}</h2>
          <p>来自笔记 #{{ knowledge.noteId }} · {{ knowledge.createTime }}</p>
        </div>
        <div class="tag-row">
          <DifficultyTag :difficulty="knowledge.difficulty" />
          <KnowledgeLevelTag :level="knowledge.masteryLevel" />
        </div>
      </article>

      <el-descriptions class="detail-meta" :column="3" border>
        <el-descriptions-item label="类型">{{ knowledge.type || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下次复习">{{ knowledge.nextReviewTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ knowledge.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-card shadow="never">
        <template #header>知识点摘要</template>
        <MarkdownPreview :content="knowledge.summary" />
      </el-card>
    </template>

    <KnowledgeEditDialog
      :knowledge="editingKnowledge"
      @saved="onEditSaved"
      @close="editingKnowledge = null"
    />

    <el-dialog
      v-model="showStreamDialog"
      title="AI 正在生成题目..."
      width="600px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="stream-output">
        <el-icon class="is-loading stream-spinner" :size="20">
          <Loading />
        </el-icon>
        <pre class="stream-text">{{ aiStreamText || '正在连接 AI 服务...' }}</pre>
      </div>
      <template #footer>
        <el-button type="danger" @click="cancelGeneration">取消生成</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.stream-output {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  max-height: 300px;
  overflow-y: auto;
}

.stream-spinner {
  margin-top: 2px;
  flex-shrink: 0;
}

.stream-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  flex: 1;
}
</style>
