<template>
  <div style="position: fixed; bottom: 100px; right: 30px; width: 380px; height: 520px; background: #fff; border-radius: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); z-index: 9998; overflow: hidden;">
    <div style="padding: 18px; background: linear-gradient(135deg, #4f46e5, #3b82f6); color: #fff; font-weight: bold; font-size: 16px;">
      🤖 AI 笔记分析助手
    </div>

    <div style="height: 390px; padding: 16px; overflow-y: auto; background: #fafafa;">
      <div v-for="(item, idx) in aiStore.messageList" :key="idx" style="margin-bottom: 12px;">
        <div v-if="item.role === 'user'" style="text-align: right;">
          <span style="background: #3b82f6; color: white; padding: 8px 12px; border-radius: 12px 12px 0 12px; display: inline-block; max-width: 70%;">
            {{ item.content }}
          </span>
        </div>
        <div v-else style="text-align: left;">
          <span style="background: white; border: 1px solid #e2e8f0; padding: 8px 12px; border-radius: 12px 12px 12px 0; display: inline-block; max-width: 70%; white-space: pre-wrap;">
            {{ item.content }}
          </span>
        </div>
      </div>
    </div>

    <div style="border-top: 1px solid #e2e8f0; background: white;">
      <el-input v-model="inputText" placeholder="输入你想分析的内容..." @keyup.enter="send" size="large" style="border: none;" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAiStore } from '@/stores/aiStore'
import { aiStreamRequest } from '@/api/ai'
const route = useRoute()
const noteId = route.params.id || ''
const aiStore = useAiStore()
const inputText = ref('')

function send() {
  if (!inputText.value.trim()) return
  if (!noteId) { alert('请先打开笔记'); return }

  const userMsg = inputText.value
  aiStore.addMessage({ role: 'user', content: userMsg })
  inputText.value = ''

  const aiMsg = { role: 'ai', content: '' }
  aiStore.addMessage(aiMsg)

  aiStreamRequest(noteId, userMsg, (chunk) => {
    aiMsg.content += chunk
  })
}
</script>