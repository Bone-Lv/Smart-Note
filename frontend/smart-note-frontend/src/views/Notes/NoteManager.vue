<template>
  <div>
    <h2 style="margin-bottom: 20px; color: #1e293b;">📙 笔记列表</h2>

    <div v-for="note in list" :key="note.id"
         style="padding: 16px; border-radius: 10px; background: #fafafa; margin-bottom: 10px; cursor: pointer; transition: 0.2s;"
         @click="goDetail(note.id)"
         @mouseover="$event.target.style.background='#eff6ff'"
         @mouseout="$event.target.style.background='#fafafa'">
      <div style="font-size: 16px; font-weight: 500; color: #374151;">{{ note.title }}</div>
    </div>

    <NoteSkeleton v-if="loading" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { noteApi } from '@/api/note'
import NoteSkeleton from '@/components/NoteSkeleton.vue'
const router = useRouter()
const list = ref([])
const loading = ref(true)

onMounted(async () => {
  const res = await noteApi.getNoteList()
  list.value = res.data
  loading.value = false
})

function goDetail(id) {
  router.push(`/note/${id}`)
}
</script>