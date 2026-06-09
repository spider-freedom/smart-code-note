<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Loading, MagicStick, Refresh } from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/knowledge'
import { noteApi } from '@/api/note'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import type { NoteDetail } from '@/types/note'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const reparsing = ref(false)
const deleting = ref(false)
const generatingKnowledge = ref(false)
const aiStreamText = ref('')
const showStreamDialog = ref(false)
const note = ref<NoteDetail | null>(null)
let abortController: AbortController | null = null

const noteId = computed(() => route.params.id as string)

const parseStatusText = computed(() => (note.value?.parseStatus === 1 ? '解析成功' : '待处理'))
const parseStatusType = computed(() => (note.value?.parseStatus === 1 ? 'success' : 'warning'))

const loadDetail = async () => {
  loading.value = true
  try {
    note.value = await noteApi.detail(noteId.value)
  } finally {
    loading.value = false
  }
}

const reparse = async () => {
  reparsing.value = true
  try {
    note.value = await noteApi.reparse(noteId.value)
    ElMessage.success('重新解析完成')
  } finally {
    reparsing.value = false
  }
}

const deleteNote = async () => {
  if (!note.value) return

  await ElMessageBox.confirm(`确定删除笔记「${note.value.title}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })

  deleting.value = true
  try {
    await noteApi.delete(noteId.value)
    ElMessage.success('笔记已删除')
    router.push('/notes')
  } finally {
    deleting.value = false
  }
}

const generateKnowledge = () => {
  if (!note.value) return

  generatingKnowledge.value = true
  aiStreamText.value = ''
  showStreamDialog.value = true

  abortController = knowledgeApi.generateStream(
    { noteId: note.value.id },
    (chunk) => {
      aiStreamText.value += chunk
    },
    (result) => {
      generatingKnowledge.value = false
      showStreamDialog.value = false
      ElMessage.success(`已生成 ${result.length} 个知识点`)
      router.push(`/knowledge?noteId=${note.value!.id}`)
    },
    (error) => {
      generatingKnowledge.value = false
      ElMessage.error(error)
    },
  )
}

const cancelGeneration = () => {
  abortController?.abort()
  generatingKnowledge.value = false
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
      <el-button :icon="ArrowLeft" @click="router.push('/notes')">返回列表</el-button>
      <div class="detail-page__actions">
        <el-button :icon="MagicStick" :loading="generatingKnowledge" @click="generateKnowledge">
          生成知识点
        </el-button>
        <el-button :icon="Refresh" :loading="reparsing" @click="reparse">重新解析</el-button>
        <el-button type="danger" :icon="Delete" :loading="deleting" @click="deleteNote">删除</el-button>
      </div>
    </div>

    <el-empty v-if="!note && !loading" description="笔记不存在" />

    <template v-if="note">
      <article class="detail-title-card">
        <div>
          <h2>{{ note.title }}</h2>
          <p>{{ note.category || '未分类' }} · {{ note.fileType }} · {{ note.createTime }}</p>
        </div>
        <el-tag :type="parseStatusType">{{ parseStatusText }}</el-tag>
      </article>

      <el-descriptions class="detail-meta" :column="3" border>
        <el-descriptions-item label="标签">{{ note.tags || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件路径">{{ note.fileUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ note.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs class="content-tabs" type="border-card">
        <el-tab-pane label="清洗内容">
          <MarkdownPreview :content="note.cleanContent" />
        </el-tab-pane>
        <el-tab-pane label="原始内容">
          <MarkdownPreview :content="note.originalContent" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog
      v-model="showStreamDialog"
      title="AI 正在分析笔记..."
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
