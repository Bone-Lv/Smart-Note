import { defineStore } from 'pinia';
import { aiApi } from '../api/ai.js';

export const useAiStore = defineStore('ai', {
  state: () => ({
    conversations: [],
    currentConversationId: null,
    messages: {},
    isLoading: false,
    isVisible: false
  }),

  getters: {
    currentMessages: (state) => {
      return state.messages[state.currentConversationId] || [];
    }
  },

  actions: {
    async fetchConversations() {
      try {
        const response = await aiApi.getConversations();
        this.conversations = response.data;
        return response;
      } catch (error) {
        throw error;
      }
    },

    async fetchChatHistory(conversationId, cursor = null, pageSize = 20) {
      try {
        const queryDTO = {
          conversationId,
          pageSize,
          cursor: cursor || undefined
        };
        
        const response = await aiApi.getChatHistory(queryDTO);
        
        if (!this.messages[conversationId]) {
          this.messages[conversationId] = [];
        }
        
        // 添加到现有消息数组开头
        this.messages[conversationId] = [
          ...response.data.records,
          ...this.messages[conversationId]
        ];
        
        return response;
      } catch (error) {
        throw error;
      }
    },

    async sendMessage(content, conversationId = null) {
      this.isLoading = true;
      
      try {
        const messageData = {
          content,
          conversationId: conversationId || this.currentConversationId
        };
        
        // 发送请求获取流式响应
        const response = await aiApi.sendMessage(messageData);
        
        // 处理流式响应
        let aiResponse = '';
        for (let i = 0; i < response.data.length; i++) {
          aiResponse += response.data[i];
        }
        
        // 添加用户消息
        const userMessage = {
          id: Date.now() + Math.random(),
          role: 'user',
          content: content,
          createTime: new Date().toISOString()
        };
        
        // 添加AI消息
        const aiMessage = {
          id: Date.now() + Math.random() + 1,
          role: 'assistant',
          content: aiResponse,
          createTime: new Date().toISOString()
        };
        
        if (!this.messages[this.currentConversationId]) {
          this.messages[this.currentConversationId] = [];
        }
        
        this.messages[this.currentConversationId].push(userMessage, aiMessage);
        
        return { userMessage, aiMessage };
      } catch (error) {
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    setCurrentConversation(conversationId) {
      this.currentConversationId = conversationId;
    },

    setShowAIChat(visible) {
      this.isVisible = visible;
    },

    async deleteConversation(conversationId) {
      try {
        await aiApi.deleteConversation(conversationId);
        this.conversations = this.conversations.filter(conv => conv.conversationId !== conversationId);
        if (this.currentConversationId === conversationId) {
          this.currentConversationId = null;
        }
        delete this.messages[conversationId];
        return true;
      } catch (error) {
        throw error;
      }
    }
  }
});