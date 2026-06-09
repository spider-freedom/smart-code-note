<template>
  <!-- 悬浮按钮 -->
  <view class="chat-fab" @tap="togglePanel" v-if="!panelVisible">
    <text class="fab-icon">💬</text>
  </view>

  <!-- 聊天面板 -->
  <view class="chat-panel" v-if="panelVisible">
    <view class="panel-header">
      <text class="panel-title">小码 AI</text>
      <view class="panel-header-actions">
        <text class="header-btn" @tap="chatStore.clearMessages()">清空</text>
        <text class="header-btn" @tap="panelVisible = false">关闭</text>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view scroll-y class="message-list" :scroll-into-view="lastMsgId">
      <view v-if="chatStore.messages.length === 0 && !chatStore.streaming" class="welcome">
        <text class="welcome-emoji">👋</text>
        <text class="welcome-title">你好！我是小码</text>
        <text class="welcome-desc">你的 AI 编程学习伙伴，有什么可以帮你的吗？</text>
      </view>

      <view v-for="(msg, index) in chatStore.messages" :key="index" :id="'msg-' + index">
        <view class="message-row" :class="msg.role === 'user' ? 'user' : 'assistant'">
          <view class="message-bubble" :class="msg.role">
            <text class="message-text">{{ msg.content }}</text>
          </view>
        </view>
      </view>

      <!-- 流式输出中的消息 -->
      <view v-if="chatStore.streaming" class="message-row assistant">
        <view class="message-bubble assistant">
          <text class="message-text">{{ chatStore.streamingContent || '思考中...' }}</text>
        </view>
      </view>

      <view id="msg-end"></view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-area">
      <textarea class="msg-input" v-model="inputText" placeholder="输入消息..." :maxlength="500"
                :auto-height="true" @confirm="send" />
      <button class="send-btn" @tap="send">发送</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import websocket from '@/utils/websocket'
import { isLoggedIn } from '@/utils/auth'

const chatStore = useChatStore()
const panelVisible = ref(false)
const inputText = ref('')
const lastMsgId = ref('msg-end')

watch(() => chatStore.messages.length, () => {
  lastMsgId.value = 'msg-end'
})

watch(() => chatStore.streamingContent, () => {
  lastMsgId.value = 'msg-end'
})

onMounted(() => {
  websocket.on('chunk', (content) => {
    chatStore.appendChunk(content)
  })
  websocket.on('result', (result) => {
    chatStore.finishStream(result)
  })
  websocket.on('error', (err) => {
    chatStore.handleError(err)
  })
  websocket.on('open', () => {
    chatStore.loadSessions()
  })
})

function togglePanel() {
  if (!isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  panelVisible.value = !panelVisible.value
  if (panelVisible.value && !websocket.connected) {
    websocket.connect()
  }
}

function send() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  chatStore.sendMessage(text)
}
</script>

<style scoped>
.chat-fab {
  position: fixed;
  right: 40rpx;
  bottom: 200rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1d4ed8, #3730a3);
  box-shadow: 0 8rpx 24rpx rgba(29, 78, 216, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99;
}

.fab-icon {
  font-size: 48rpx;
}

.chat-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 32rpx;
  background: linear-gradient(135deg, #1d4ed8, #3730a3);
  color: #fff;
}

.panel-title {
  font-size: 34rpx;
  font-weight: 700;
}

.panel-header-actions {
  display: flex;
  gap: 24rpx;
}

.header-btn {
  font-size: 28rpx;
  padding: 8rpx 20rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
}

.message-list {
  flex: 1;
  padding: 24rpx;
  background: #f9fafb;
}

.welcome {
  text-align: center;
  padding-top: 80rpx;
}

.welcome-emoji {
  font-size: 80rpx;
  display: block;
  margin-bottom: 24rpx;
}

.welcome-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
  display: block;
  margin-bottom: 12rpx;
}

.welcome-desc {
  font-size: 26rpx;
  color: #9ca3af;
  line-height: 1.6;
}

.message-row {
  margin-bottom: 24rpx;
}

.message-row.user {
  display: flex;
  justify-content: flex-end;
}

.message-bubble {
  max-width: 80%;
  padding: 16rpx 24rpx;
  border-radius: 20rpx;
  font-size: 28rpx;
  line-height: 1.6;
}

.message-bubble.user {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
  border-bottom-right-radius: 6rpx;
}

.message-bubble.assistant {
  background: #fff;
  color: #374151;
  border-bottom-left-radius: 6rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #f3f4f6;
}

.msg-input {
  flex: 1;
  min-height: 72rpx;
  max-height: 160rpx;
  background: #f9fafb;
  border-radius: 36rpx;
  padding: 14rpx 24rpx;
  font-size: 28rpx;
}

.send-btn {
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
  flex-shrink: 0;
}
</style>
