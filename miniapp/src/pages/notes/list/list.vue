<template>
  <view class="page-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索笔记..." confirm-type="search" @confirm="search" />
      <button class="upload-btn" @tap="showUpload = true">上传</button>
    </view>

    <!-- 分类筛选 -->
    <scroll-view scroll-x class="category-bar">
      <view class="category-tag" :class="{ active: !currentCategory }" @tap="selectCategory(null)">全部</view>
      <view class="category-tag" v-for="cat in categories" :key="cat" :class="{ active: currentCategory === cat }" @tap="selectCategory(cat)">{{ cat }}</view>
    </scroll-view>

    <!-- 笔记列表 -->
    <view v-if="notes.length" class="note-list">
      <view class="note-card card" v-for="note in notes" :key="note.id" @tap="goDetail(note.id)">
        <view class="note-header">
          <text class="note-title text-ellipsis">{{ note.title }}</text>
          <text class="note-type">{{ note.fileType }}</text>
        </view>
        <view class="note-meta">
          <text class="note-category" v-if="note.category">{{ note.category }}</text>
          <text class="note-tags" v-if="note.tags">{{ note.tags }}</text>
        </view>
        <view class="note-footer">
          <text class="note-date">{{ formatDate(note.createTime) }}</text>
          <text class="note-status" :class="{ success: note.parseStatus === 1 }">
            {{ note.parseStatus === 1 ? '已解析' : '未解析' }}
          </text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📄</text>
      <text class="empty-text">还没有笔记，点击上传开始吧</text>
    </view>

    <!-- 上传弹窗 -->
    <view class="popup-mask" v-if="showUpload" @tap="showUpload = false">
      <view class="popup-panel" @tap.stop>
        <text class="popup-title">上传笔记</text>

        <view class="form-group">
          <text class="form-label">标题</text>
          <input class="form-input" v-model="uploadForm.title" placeholder="请输入笔记标题" />
        </view>

        <view class="form-group">
          <text class="form-label">内容</text>
          <textarea class="form-textarea" v-model="uploadForm.content" placeholder="请粘贴笔记内容，或点击下方按钮选择文件" />
        </view>

        <button class="choose-file-btn" @tap="chooseFile">从文件选择</button>

        <view class="form-group">
          <text class="form-label">分类 (可选)</text>
          <input class="form-input" v-model="uploadForm.category" placeholder="如: Java, Python" />
        </view>

        <view class="form-group">
          <text class="form-label">标签 (可选)</text>
          <input class="form-input" v-model="uploadForm.tags" placeholder="如: 入门, 进阶" />
        </view>

        <view class="popup-actions">
          <button class="btn-cancel" @tap="showUpload = false">取消</button>
          <button class="btn-submit" @tap="handleUpload" :loading="uploading">上传</button>
        </view>
      </view>
    </view>

    <!-- 加载更多 -->
    <view class="load-more" v-if="hasMore">
      <text class="load-more-text" @tap="loadMore">加载更多</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { noteApi } from '@/api/note'

const notes = ref([])
const keyword = ref('')
const currentCategory = ref(null)
const categories = ref([])
const pageNum = ref(1)
const hasMore = ref(false)
const showUpload = ref(false)
const uploading = ref(false)

const uploadForm = ref({
  title: '',
  content: '',
  category: '',
  tags: ''
})

onMounted(() => {
  fetchNotes()
})

async function fetchNotes(reset = false) {
  if (reset) {
    pageNum.value = 1
    notes.value = []
  }
  try {
    const result = await noteApi.list({
      pageNum: pageNum.value,
      pageSize: 10,
      keyword: keyword.value || undefined,
      category: currentCategory.value || undefined
    })
    if (reset) {
      notes.value = result.records || []
    } else {
      notes.value.push(...(result.records || []))
    }
    hasMore.value = result.records && result.records.length === 10
    const cats = new Set(notes.value.map(n => n.category).filter(Boolean))
    categories.value = [...cats]
  } catch (e) { /* ignore */ }
}

function search() {
  fetchNotes(true)
}

function selectCategory(cat) {
  currentCategory.value = cat
  fetchNotes(true)
}

function loadMore() {
  pageNum.value++
  fetchNotes()
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/notes/detail/detail?id=${id}` })
}

async function handleUpload() {
  if (!uploadForm.value.title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  if (!uploadForm.value.content) {
    uni.showToast({ title: '请输入内容', icon: 'none' })
    return
  }
  uploading.value = true
  try {
    await noteApi.uploadText({
      title: uploadForm.value.title,
      content: uploadForm.value.content,
      category: uploadForm.value.category || undefined,
      tags: uploadForm.value.tags || undefined
    })
    uni.showToast({ title: '上传成功', icon: 'success' })
    showUpload.value = false
    uploadForm.value = { title: '', content: '', category: '', tags: '' }
    fetchNotes(true)
  } catch (e) { /* ignore */ } finally {
    uploading.value = false
  }
}

function chooseFile() {
  wx.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['txt', 'md'],
    success: (res) => {
      const file = res.tempFiles[0]
      const fs = wx.getFileSystemManager()
      const content = fs.readFileSync(file.path, 'utf8')
      uploadForm.value.content = content
      if (!uploadForm.value.title && file.name) {
        uploadForm.value.title = file.name.replace(/\.(txt|md)$/i, '')
      }
    },
    fail: (err) => {
      if (err.errMsg.indexOf('cancel') === -1) {
        uni.showToast({ title: '选择文件失败', icon: 'none' })
      }
    }
  })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  background: #fff;
  border-radius: 36rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.upload-btn {
  width: 120rpx;
  height: 72rpx;
  line-height: 72rpx;
  background: #1d4ed8;
  color: #fff;
  border-radius: 36rpx;
  font-size: 28rpx;
  border: none;
  padding: 0;
  text-align: center;
}

.category-bar {
  white-space: nowrap;
  margin-bottom: 20rpx;
  padding-bottom: 8rpx;
}

.category-tag {
  display: inline-block;
  padding: 8rpx 24rpx;
  margin-right: 16rpx;
  background: #fff;
  border-radius: 20rpx;
  font-size: 24rpx;
  color: #6b7280;
  box-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.04);
}

.category-tag.active {
  background: #dbeafe;
  color: #1d4ed8;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.note-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
  flex: 1;
}

.note-type {
  font-size: 22rpx;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

.note-meta {
  display: flex;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.note-category, .note-tags {
  font-size: 22rpx;
  color: #6b7280;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note-date {
  font-size: 22rpx;
  color: #9ca3af;
}

.note-status {
  font-size: 22rpx;
  color: #9ca3af;
}

.note-status.success {
  color: #10b981;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #9ca3af;
}

.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}

.popup-panel {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 32rpx 32rpx 48rpx;
  width: 100%;
  max-height: 85vh;
  overflow-y: auto;
}

.popup-title {
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  display: block;
  margin-bottom: 32rpx;
}

.form-group {
  margin-bottom: 20rpx;
}

.form-label {
  font-size: 26rpx;
  color: #374151;
  margin-bottom: 8rpx;
  display: block;
}

.form-input {
  height: 72rpx;
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  border: 1rpx solid #e5e7eb;
}

.form-textarea {
  min-height: 200rpx;
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 26rpx;
  border: 1rpx solid #e5e7eb;
}

.choose-file-btn {
  width: 100%;
  height: 72rpx;
  line-height: 72rpx;
  background: #f3f4f6;
  color: #374151;
  border-radius: 12rpx;
  font-size: 26rpx;
  border: none;
  margin-bottom: 20rpx;
  text-align: center;
}

.popup-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.btn-cancel, .btn-submit {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
}

.btn-cancel {
  background: #f3f4f6;
  color: #6b7280;
}

.btn-submit {
  background: #1d4ed8;
  color: #fff;
}

.load-more {
  text-align: center;
  padding: 24rpx;
}

.load-more-text {
  font-size: 26rpx;
  color: #2563eb;
}
</style>
