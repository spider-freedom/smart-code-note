<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { ChatDotRound, Close, Delete, Plus, Select, Sunny } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import MarkdownPreview from '@/components/MarkdownPreview.vue'

const store = useChatStore()
const input = ref('')
const messagesEl = ref<HTMLElement | null>(null)
const showSessions = ref(false)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

watch(() => store.messages.length, scrollToBottom)

const handleSend = () => {
  if (!input.value.trim().length || store.isStreaming) return
  store.sendMessage(input.value)
  input.value = ''
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const switchSession = (id: number) => {
  store.loadSession(id)
  showSessions.value = false
}

const startNewChat = () => {
  store.newSession()
  showSessions.value = false
}

const clearCurrentChat = async () => {
  try {
    await ElMessageBox.confirm('确定要清空当前对话吗？', '清空确认', {
      confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning',
    })
    store.newSession()
  } catch { /* cancelled */ }
}

const showDateDivider = (index: number) => {
  if (index === 0) return true
  const current = new Date(store.messages[index].createTime || '')
  const previous = new Date(store.messages[index - 1].createTime || '')
  return current.toDateString() !== previous.toDateString()
}

const dateLabel = (index: number) => {
  const d = new Date(store.messages[index].createTime || '')
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  return d.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}

onMounted(() => {
  store.loadSessions()
})
</script>

<template>
  <div class="chat-bubble" :class="{ 'chat-bubble--open': store.isOpen }">
    <el-tooltip :content="store.isOpen ? '关闭对话' : '和小码聊天'" placement="left">
      <button class="chat-bubble__fab" @click="store.toggleOpen()">
        <el-icon v-if="!store.isOpen" :size="28"><ChatDotRound /></el-icon>
        <el-icon v-else :size="24"><Close /></el-icon>
      </button>
    </el-tooltip>

    <transition name="chat-panel">
      <div v-if="store.isOpen" class="chat-bubble__panel">
        <div class="chat-panel__header">
          <div class="chat-panel__title">
            <span class="chat-panel__avatar">🤖</span>
            <div>
              <strong>小码</strong>
              <small>你的 AI 学习伙伴</small>
            </div>
          </div>
          <div class="chat-panel__header-actions">
            <el-tooltip content="新对话" placement="bottom">
              <el-button :icon="Plus" size="small" circle @click="startNewChat" />
            </el-tooltip>
            <el-tooltip content="清空对话" placement="bottom">
              <el-button :icon="Sunny" size="small" circle @click="clearCurrentChat" />
            </el-tooltip>
            <el-tooltip content="历史会话" placement="bottom">
              <el-button :icon="Select" size="small" circle @click="showSessions = !showSessions" />
            </el-tooltip>
          </div>
        </div>

        <transition name="slide">
          <div v-if="showSessions" class="chat-panel__sessions">
            <div class="chat-panel__sessions-header">
              <span>历史会话</span>
              <el-button size="small" type="primary" text :icon="Plus" @click="startNewChat">新对话</el-button>
            </div>
            <div
              v-for="s in store.sessions"
              :key="s.id"
              class="chat-panel__session-item"
              :class="{ active: s.id === store.currentSessionId }"
              @click="switchSession(s.id)"
            >
              <span class="session-title">{{ s.title }}</span>
              <el-tooltip content="删除会话" placement="left">
                <el-button
                  :icon="Delete"
                  size="small"
                  text
                  @click.stop="store.deleteSessionAndRefresh(s.id)"
                />
              </el-tooltip>
            </div>
            <el-empty v-if="!store.sessions.length" description="暂无历史会话" :image-size="48" />
          </div>
        </transition>

        <div ref="messagesEl" class="chat-panel__messages">
          <div v-if="!store.messages.length" class="chat-panel__welcome">
            <p class="welcome-emoji">👋</p>
            <p class="welcome-text">嗨！我是 <strong>小码</strong></p>
            <p class="welcome-sub">你的 AI 学习伙伴，随时陪你学习、解答疑惑。</p>
            <p class="welcome-hint">学习中遇到问题？直接问我吧～</p>
          </div>

          <template v-for="(msg, i) in store.messages" :key="msg.id">
            <div v-if="showDateDivider(i)" class="chat-date-divider">
              <span>{{ dateLabel(i) }}</span>
            </div>
            <div class="chat-message" :class="'chat-message--' + msg.role">
              <div class="chat-message__avatar">
                <span v-if="msg.role === 'user'">👤</span>
                <span v-else>🤖</span>
              </div>
              <div class="chat-message__bubble">
                <template v-if="msg.role === 'assistant'">
                  <MarkdownPreview :content="msg.content || '小码正在思考...'" />
                  <span v-if="msg.id === store.messages[store.messages.length - 1]?.id && store.isStreaming" class="typing-cursor">|</span>
                </template>
                <template v-else>
                  {{ msg.content }}
                </template>
              </div>
            </div>
          </template>
        </div>

        <div class="chat-panel__input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行..."
            :disabled="store.isStreaming"
            @keydown="handleKeydown"
          />
          <el-tooltip content="发送消息" placement="top">
            <el-button
              type="primary"
              :icon="ChatDotRound"
              :loading="store.isStreaming"
              :disabled="!input.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.chat-bubble {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  font-family: inherit;
}

.chat-bubble__fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  margin-left: auto;
}

.chat-bubble__fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.55);
}

.chat-bubble__panel {
  position: absolute;
  right: 0;
  bottom: 72px;
  width: 420px;
  height: 600px;
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.14);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  flex-shrink: 0;
}

.chat-panel__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-panel__title div {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.chat-panel__title strong {
  font-size: 16px;
}

.chat-panel__title small {
  font-size: 12px;
  opacity: 0.85;
}

.chat-panel__avatar {
  font-size: 28px;
}

.chat-panel__header-actions {
  display: flex;
  gap: 4px;
}

.chat-panel__header-actions .el-button {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}

.chat-panel__header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
}

.chat-panel__sessions {
  max-height: 200px;
  overflow-y: auto;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.chat-panel__sessions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.chat-panel__session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  transition: background 0.15s;
}

.chat-panel__session-item:hover {
  background: var(--el-fill-color);
}

.chat-panel__session-item.active {
  background: var(--el-color-primary-light-9);
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-panel__welcome {
  text-align: center;
  margin: auto;
  color: var(--el-text-color-secondary);
}

.welcome-emoji {
  font-size: 56px;
  margin-bottom: 12px;
}

.welcome-text {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--el-text-color-primary);
}

.welcome-sub {
  font-size: 13px;
  margin: 0 0 12px;
  color: var(--el-text-color-regular);
}

.welcome-hint {
  font-size: 13px;
  margin: 0;
  color: var(--el-text-color-placeholder);
}

.chat-date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 4px 0;
}

.chat-date-divider span {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  background: var(--el-bg-color);
  padding: 2px 12px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-extra-light);
}

.chat-message {
  display: flex;
  gap: 8px;
  max-width: 90%;
}

.chat-message--user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat-message--assistant {
  align-self: flex-start;
}

.chat-message__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: var(--el-fill-color-light);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.chat-message__bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-message--user .chat-message__bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.25);
}

.chat-message--assistant .chat-message__bubble {
  background: var(--el-fill-color-light);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chat-message--assistant .chat-message__bubble :deep(p) {
  margin: 0;
}

.chat-message--assistant .chat-message__bubble :deep(p + p) {
  margin-top: 6px;
}

.typing-cursor {
  animation: blink 0.8s infinite;
  font-weight: bold;
  color: var(--el-color-primary);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.chat-panel__input {
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 8px;
  align-items: flex-end;
  flex-shrink: 0;
}

.chat-panel__input .el-textarea {
  flex: 1;
}

.chat-panel-enter-active {
  transition: all 0.25s ease-out;
}

.chat-panel-leave-active {
  transition: all 0.2s ease-in;
}

.chat-panel-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}

.chat-panel-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.97);
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.2s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
