<template>
  <div>
    <h2>好友列表</h2>
    <div style="margin-top: 20px;">
      <div
        v-for="friend in list"
        :key="friend.id"
        style="padding: 12px; border-bottom: 1px solid #eee;"
      >
        {{ friend.username }}
      </div>
      <NoteSkeleton v-if="loading" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFriendList } from '@/api/social'
import NoteSkeleton from '@/components/NoteSkeleton.vue'

const list = ref([])
const loading = ref(true)

onMounted(async () => {
  const res = await getFriendList()
  list.value = res.data
  loading.value = false
})
</script>