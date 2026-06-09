<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Delete, Edit, MagicStick, Plus, Search } from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/knowledge'
import { questionApi } from '@/api/question'
import DifficultyTag from '@/components/DifficultyTag.vue'
import KnowledgeEditDialog from '@/components/KnowledgeEditDialog.vue'
import KnowledgeLevelTag from '@/components/KnowledgeLevelTag.vue'
import type { KnowledgeDetail, KnowledgeListItem } from '@/types/knowledge'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const deletingId = ref<number | null>(null)
const generatingQuestionId = ref<number | null>(null)
const generatingKnowledge = ref(false)
const generateVisible = ref(false)
const generateFormRef = ref<FormInstance>()
const knowledgeList = ref<KnowledgeListItem[]>([])
const total = ref(0)
const editingKnowledge = ref<KnowledgeDetail | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  noteId: undefined as number | undefined,
  keyword: '',
  type: '',
  difficulty: '',
  masteryLevel: undefined as number | undefined,
})

const generateForm = reactive({
  noteId: undefined as number | undefined,
})

const generateRules = {
  noteId: [{ required: true, message: '请输入笔记 ID', trigger: 'blur' }],
}

const loadKnowledge = async () => {
  loading.value = true
  try {
    syncQueryToUrl()
    const result = await knowledgeApi.list({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      noteId: query.noteId,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      difficulty: query.difficulty || undefined,
      masteryLevel: query.masteryLevel,
    })
    knowledgeList.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const syncQueryToUrl = () => {
  router.replace({
    path: '/knowledge',
    query: {
      pageNum: query.pageNum === 1 ? undefined : String(query.pageNum),
      pageSize: query.pageSize === 10 ? undefined : String(query.pageSize),
      noteId: query.noteId ? String(query.noteId) : undefined,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      difficulty: query.difficulty || undefined,
      masteryLevel: query.masteryLevel === undefined ? undefined : String(query.masteryLevel),
    },
  })
}

const search = () => {
  query.pageNum = 1
  loadKnowledge()
}

const reset = () => {
  query.pageNum = 1
  query.keyword = ''
  query.type = ''
  query.difficulty = ''
  query.noteId = undefined
  query.masteryLevel = undefined
  loadKnowledge()
}

const handlePageChange = (pageNum: number) => {
  query.pageNum = pageNum
  loadKnowledge()
}

const handleSizeChange = (pageSize: number) => {
  query.pageSize = pageSize
  query.pageNum = 1
  loadKnowledge()
}

const openEdit = async (row: KnowledgeListItem) => {
  editingKnowledge.value = await knowledgeApi.detail(row.id)
}

const onEditSaved = () => {
  editingKnowledge.value = null
  loadKnowledge()
}

const deleteKnowledge = async (row: KnowledgeListItem) => {
  await ElMessageBox.confirm(`确定删除知识点「${row.title}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })

  deletingId.value = row.id
  try {
    await knowledgeApi.delete(row.id)
    ElMessage.success('知识点已删除')
    if (knowledgeList.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadKnowledge()
  } finally {
    deletingId.value = null
  }
}

const generateKnowledge = async () => {
  if (!generateFormRef.value) return

  const valid = await generateFormRef.value.validate().catch(() => false)
  if (!valid || !generateForm.noteId) return

  generatingKnowledge.value = true
  try {
    const result = await knowledgeApi.generate({ noteId: generateForm.noteId })
    ElMessage.success(`已生成 ${result.length} 个知识点`)
    generateVisible.value = false
    query.noteId = generateForm.noteId
    query.pageNum = 1
    loadKnowledge()
  } finally {
    generatingKnowledge.value = false
  }
}

const generateQuestions = async (row: KnowledgeListItem) => {
  generatingQuestionId.value = row.id
  try {
    const result = await questionApi.generate({ knowledgeId: row.id, count: 3 })
    ElMessage.success(`已生成 ${result.length} 道题目`)
    router.push(`/questions?knowledgeId=${row.id}`)
  } finally {
    generatingQuestionId.value = null
  }
}

onMounted(() => {
  const noteId = Number(route.query.noteId)
  if (Number.isFinite(noteId) && noteId > 0) {
    query.noteId = noteId
    generateForm.noteId = noteId
  }
  query.pageNum = Number(route.query.pageNum) || 1
  query.pageSize = Number(route.query.pageSize) || 10
  query.keyword = (route.query.keyword as string) || ''
  query.type = (route.query.type as string) || ''
  query.difficulty = (route.query.difficulty as string) || ''
  const masteryLevel = Number(route.query.masteryLevel)
  if (Number.isFinite(masteryLevel)) {
    query.masteryLevel = masteryLevel
  }
  loadKnowledge()
})
</script>

<template>
  <section class="table-page">
    <div class="table-page__header">
      <div>
        <h2>知识点</h2>
        <p>筛选、编辑和维护从开发笔记中提取出的复习知识点。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="generateVisible = true">从笔记生成</el-button>
    </div>

    <el-form class="filter-bar" :model="query" inline @submit.prevent="search">
      <el-form-item label="笔记 ID">
        <el-input-number v-model="query.noteId" :min="1" controls-position="right" placeholder="noteId" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题/摘要" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="类型">
        <el-input v-model.trim="query.type" clearable placeholder="概念/配置/代码" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="难度">
        <el-input v-model.trim="query.difficulty" clearable placeholder="简单/中等/困难" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="掌握度">
        <el-select v-model="query.masteryLevel" clearable placeholder="全部" style="width: 130px">
          <el-option v-for="level in [0, 1, 2, 3, 4, 5]" :key="level" :label="`${level}`" :value="level" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="knowledgeList" border>
      <el-table-column prop="title" label="标题" min-width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/knowledge/${row.id}`)">{{ row.title }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="noteId" label="笔记 ID" width="100" />
      <el-table-column prop="type" label="类型" width="130">
        <template #default="{ row }">{{ row.type || '-' }}</template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="120">
        <template #default="{ row }"><DifficultyTag :difficulty="row.difficulty" /></template>
      </el-table-column>
      <el-table-column prop="masteryLevel" label="掌握程度" width="130">
        <template #default="{ row }"><KnowledgeLevelTag :level="row.masteryLevel" /></template>
      </el-table-column>
      <el-table-column prop="nextReviewTime" label="下次复习" min-width="170">
        <template #default="{ row }">{{ row.nextReviewTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/knowledge/${row.id}`)">详情</el-button>
          <el-button link :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button link :icon="MagicStick" :loading="generatingQuestionId === row.id" @click="generateQuestions(row)">
            生成题目
          </el-button>
          <el-button link type="danger" :icon="Delete" :loading="deletingId === row.id" @click="deleteKnowledge(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无知识点">
          <el-button type="primary" @click="generateVisible = true">从笔记生成知识点</el-button>
        </el-empty>
      </template>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="generateVisible" title="从笔记生成知识点" width="460px">
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-position="top">
        <el-form-item label="笔记 ID" prop="noteId">
          <el-input-number v-model="generateForm.noteId" :min="1" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generatingKnowledge" @click="generateKnowledge">生成</el-button>
      </template>
    </el-dialog>

    <KnowledgeEditDialog
      :knowledge="editingKnowledge"
      @saved="onEditSaved"
      @close="editingKnowledge = null"
    />
  </section>
</template>
