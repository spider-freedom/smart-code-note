<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadInstance,
  type UploadProps,
  type UploadRawFile,
} from 'element-plus'
import { Delete, Refresh, Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import { noteApi } from '@/api/note'
import type { NoteListItem } from '@/types/note'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const reparsingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const notes = ref<NoteListItem[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: '',
})

const uploadVisible = ref(false)
const uploadLoading = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)

const form = reactive({
  title: '',
  category: '',
  tags: '',
})

const rules: FormRules<typeof form> = {
  title: [
    { required: true, message: '请输入笔记标题', trigger: 'blur' },
    { max: 120, message: '标题不宜超过 120 个字符', trigger: 'blur' },
  ],
  category: [{ max: 64, message: '分类不能超过 64 个字符', trigger: 'blur' }],
  tags: [{ max: 255, message: '标签不能超过 255 个字符', trigger: 'blur' }],
}

const validateFile = (rawFile: File) => {
  const extension = rawFile.name.split('.').pop()?.toLowerCase()
  if (extension !== 'txt' && extension !== 'md') {
    ElMessage.error('仅支持 .txt 和 .md 文件')
    return false
  }
  if (rawFile.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

const handleFileChange: UploadProps['onChange'] = (uploadFile: UploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile) { selectedFile.value = null; return }
  if (!validateFile(rawFile)) { selectedFile.value = null; uploadRef.value?.clearFiles(); return }
  selectedFile.value = rawFile
  if (!form.title) form.title = rawFile.name.replace(/\.[^.]+$/, '')
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile: UploadRawFile) => validateFile(rawFile)

const removeFile = () => {
  selectedFile.value = null
}

const openUpload = () => {
  form.title = ''
  form.category = ''
  form.tags = ''
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  uploadVisible.value = true
}

const submitUpload = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!selectedFile.value) { ElMessage.warning('请先选择笔记文件'); return }

  uploadLoading.value = true
  try {
    const result = await noteApi.upload({
      file: selectedFile.value,
      title: form.title,
      category: form.category || undefined,
      tags: form.tags || undefined,
    })
    ElMessage.success('笔记上传成功')
    uploadVisible.value = false
    router.push(`/notes/${result.id}`)
  } finally {
    uploadLoading.value = false
  }
}

const parseStatusText = (status: number) => (status === 1 ? '解析成功' : '待处理')
const parseStatusType = (status: number) => (status === 1 ? 'success' : 'warning')

const loadNotes = async () => {
  loading.value = true
  try {
    syncQueryToUrl()
    const result = await noteApi.list({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
    })
    notes.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const syncQueryToUrl = () => {
  router.replace({
    path: '/notes',
    query: {
      pageNum: query.pageNum === 1 ? undefined : String(query.pageNum),
      pageSize: query.pageSize === 10 ? undefined : String(query.pageSize),
      keyword: query.keyword || undefined,
      category: query.category || undefined,
    },
  })
}

const search = () => { query.pageNum = 1; loadNotes() }

const reset = () => { query.keyword = ''; query.category = ''; query.pageNum = 1; loadNotes() }

const handlePageChange = (pageNum: number) => { query.pageNum = pageNum; loadNotes() }
const handleSizeChange = (pageSize: number) => { query.pageSize = pageSize; query.pageNum = 1; loadNotes() }

const reparse = async (row: NoteListItem) => {
  reparsingId.value = row.id
  try { await noteApi.reparse(row.id); ElMessage.success('重新解析完成'); loadNotes() }
  finally { reparsingId.value = null }
}

const deleteNote = async (row: NoteListItem) => {
  await ElMessageBox.confirm(`确定删除笔记「${row.title}」吗？`, '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
  })
  deletingId.value = row.id
  try {
    await noteApi.delete(row.id); ElMessage.success('笔记已删除')
    if (notes.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadNotes()
  } finally { deletingId.value = null }
}

onMounted(() => {
  query.pageNum = Number(route.query.pageNum) || 1
  query.pageSize = Number(route.query.pageSize) || 10
  query.keyword = (route.query.keyword as string) || ''
  query.category = (route.query.category as string) || ''
  loadNotes()
})
</script>

<template>
  <section class="table-page">
    <div class="table-page__header">
      <div>
        <h2>我的笔记</h2>
        <p>管理已上传的开发笔记，查看解析状态并进入详情。</p>
      </div>
      <el-button type="primary" :icon="Upload" @click="openUpload">上传笔记</el-button>
    </div>

    <el-form class="filter-bar" :model="query" inline @submit.prevent="search">
      <el-form-item label="关键词">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题或标签" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model.trim="query.category" clearable placeholder="例如 Spring Boot" @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="notes" border>
      <el-table-column prop="title" label="标题" min-width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/notes/${row.id}`)">{{ row.title }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="140">
        <template #default="{ row }">{{ row.category || '-' }}</template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" min-width="180">
        <template #default="{ row }">{{ row.tags || '-' }}</template>
      </el-table-column>
      <el-table-column prop="fileType" label="类型" width="90" />
      <el-table-column prop="parseStatus" label="解析状态" width="110">
        <template #default="{ row }">
          <el-tag :type="parseStatusType(row.parseStatus)">{{ parseStatusText(row.parseStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/notes/${row.id}`)">详情</el-button>
          <el-button link :icon="Refresh" :loading="reparsingId === row.id" @click="reparse(row)">重新解析</el-button>
          <el-button link type="danger" :icon="Delete" :loading="deletingId === row.id" @click="deleteNote(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无笔记">
          <el-button type="primary" @click="openUpload">上传第一篇笔记</el-button>
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

    <el-dialog v-model="uploadVisible" title="上传笔记" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="笔记文件" required>
          <el-upload
            ref="uploadRef"
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            :on-remove="removeFile"
            accept=".txt,.md"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到这里，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .txt / .md，最大 20MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model.trim="form.title" placeholder="例如：Spring Boot 异常处理笔记" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model.trim="form.category" placeholder="例如：Java / MySQL / Spring Boot" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model.trim="form.tags" placeholder="多个标签可用逗号分隔，例如：事务,面试,配置" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>
  </section>
</template>
