// src/stores/aiStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { AIConversationVO, ChatMessageVO } from '@/types/api'

export const aiStore = defineStore('ai', () => {
  const conversations = ref<AIConversationVO[]>([])
  const currentConversationId = ref<string | null>(null)
  const messages = ref<Record<string, ChatMessageVO[]>>({})
  const isChatVisible = ref(false)

  const setCurrentConversation = (id: string | null) => {
    currentConversationId.value = id
  }

  const addMessage = (conversationId: string, message: ChatMessageVO) => {
    if (!messages.value[conversationId]) {
      messages.value[conversationId] = []
    }
    messages.value[conversationId].push(message)
  }

  const addConversation = (conversation: AIConversationVO) => {
    const existingIndex = conversations.value.findIndex(c => c.conversationId === conversation.conversationId)
    if (existingIndex > -1) {
      conversations.value.splice(existingIndex, 1)
    }
    conversations.value.unshift(conversation)
  }

  const removeConversation = (id: string) => {
    const index = conversations.value.findIndex(c => c.conversationId === id)
    if (index > -1) {
      conversations.value.splice(index, 1)
    }
    delete messages.value[id]
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value.length > 0 ? conversations.value[0].conversationId : null
    }
  }

  const clearAllConversations = () => {
    conversations.value = []
    messages.value = {}
    currentConversationId.value = null
  }

  const toggleChatVisibility = () => {
    isChatVisible.value = !isChatVisible.value
  }

  return {
    conversations,
    currentConversationId,
    messages,
    isChatVisible,
    setCurrentConversation,
    addMessage,
    addConversation,
    removeConversation,
    clearAllConversations,
    toggleChatVisibility
  }
})