<template>
  <div>
    <h2>编辑笔记</h2>
    
    <div style="margin: 20px 0">
      <el-input
        v-model="title"
        type="textarea"
        :rows="1"
        placeholder="笔记标题"
        style="margin-bottom: 12px"
      />

      <el-input
        v-model="text"
        type="textarea"
        :rows="10"
        placeholder="请输入 Markdown 内容"
        @input="handleUpdate"
      />
    </div>

    <div style="margin: 20px 0">
      <h3>预览（纯 Markdown）</h3>
      <MarkdownRenderer :content="text" />
    </div>

    <div style="margin-top: 12px">
      <el-button @click="showDiff = true">版本对比</el-button>
    </div>

    <DiffModal
      :show="showDiff"
      :oldContent="oldContent"
      :newContent="text"
      @update:show="showDiff = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNoteDetail, updateNote } from '@/api/note'
import { useNoteEditor } from '@/hooks/useNoteEditor'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import DiffModal from '@/components/DiffModal.vue'

const route = useRoute()
const noteId = route.params.id

const title = ref('')
const text = ref('')
const oldContent = ref('')
const showDiff = ref(false)

const { content, requestLock } = useNoteEditor(noteId)

onMounted(async () => {
  const res = await getNoteDetail(noteId)
  title.value = res.data.title
  text.value = res.data.content
  oldContent.value = res.data.content
  requestLock()
})

async function handleUpdate() {
  content.value = text.value
  await updateNote(noteId, { title: title.value, content: text.value })
}
</script>