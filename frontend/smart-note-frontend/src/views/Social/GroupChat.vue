<template>
  <div>
    <h2>群聊</h2>

    <div style="border: 1px solid #eee; height: 400px; padding: 12px; margin: 12px 0; overflow-y: auto;">
      <div v-for="msg in list" :key="msg.id" style="margin: 8px 0;">
        <b>{{ msg.senderName }}：</b>{{ msg.content }}
      </div>
    </div>

    <el-button @click="loadMore" :loading="loading">加载更多</el-button>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { getMessageList } from '@/api/social'
import { useCursorPagination } from '@/hooks/useCursorPagination'

const { list, loading, load, loadMore } = useCursorPagination(getMessageList)

onMounted(() => {
  load()
})
</script>