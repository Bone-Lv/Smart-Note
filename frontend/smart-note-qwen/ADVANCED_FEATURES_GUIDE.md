# 高级功能使用指南

## 一、离线缓存功能

### 1.1 基本用法

```javascript
import { saveNoteDraft, getNoteDraft, deleteNoteDraft } from '@/utils/cache'

// 保存笔记草稿
await saveNoteDraft(noteId, {
  title: '我的笔记',
  content: '# 内容'
})

// 获取笔记草稿
const draft = await getNoteDraft(noteId)
if (draft) {
  console.log('恢复草稿:', draft)
}

// 删除笔记草稿
await deleteNoteDraft(noteId)
```

### 1.2 在组件中使用

```vue
<script setup>
import { ref, watch } from 'vue'
import { saveNoteDraft, getNoteDraft } from '@/utils/cache'

const noteId = 123
const content = ref('')

// 自动保存草稿
watch(content, async (newContent) => {
  await saveNoteDraft(noteId, {
    title: '标题',
    content: newContent
  })
}, { deep: true })

// 加载时恢复草稿
onMounted(async () => {
  const draft = await getNoteDraft(noteId)
  if (draft) {
    content.value = draft.content
  }
})
</script>
```

### 1.3 其他缓存功能

```javascript
import { 
  saveNoteContent, 
  getNoteContent,
  saveUserInfo,
  getUserInfo,
  clearAllCache,
  getCacheStats 
} from '@/utils/cache'

// 缓存笔记内容
await saveNoteContent(noteId, '笔记内容')
const cachedContent = await getNoteContent(noteId)

// 缓存用户信息
await saveUserInfo('profile', { name: '张三' })
const profile = await getUserInfo('profile')

// 清除所有缓存
await clearAllCache()

// 获取缓存统计
const stats = await getCacheStats()
console.log('缓存统计:', stats)
// { drafts: 5, contents: 10, total: 15 }
```

---

## 二、图片懒加载

### 2.1 基本用法

```vue
<template>
  <div class="image-gallery">
    <!-- 使用 v-lazy 指令 -->
    <img 
      v-for="img in images" 
      :key="img.id"
      v-lazy="img.url"
      :alt="img.alt"
      class="lazy-image"
    />
  </div>
</template>

<script setup>
const images = [
  { id: 1, url: 'https://example.com/image1.jpg', alt: '图片1' },
  { id: 2, url: 'https://example.com/image2.jpg', alt: '图片2' }
]
</script>

<style scoped>
.lazy-image {
  width: 100%;
  height: auto;
  transition: opacity 0.3s;
}

.lazy-image.lazy {
  opacity: 0;
}

.lazy-image.loaded {
  opacity: 1;
}

.lazy-image.error {
  /* 错误状态样式 */
  background: #f5f5f5;
}
</style>
```

### 2.2 添加占位图

```vue
<template>
  <img 
    v-lazy="imageUrl"
    :alt="imageAlt"
    class="lazy-image"
    style="background-image: url('/placeholder.svg')"
  />
</template>
```

### 2.3 批量懒加载

```javascript
import { lazyLoadImages } from '@/directives/lazyLoad'

onMounted(() => {
  const images = document.querySelectorAll('.gallery img')
  lazyLoadImages(Array.from(images))
})
```

---

## 三、虚拟滚动

### 3.1 基本用法

```vue
<template>
  <div class="note-list">
    <VirtualList
      :list="notes"
      :item-height="80"
      :height="600"
      :buffer="5"
    >
      <template #default="{ item, index }">
        <div class="note-item" :key="item.id">
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary }}</p>
        </div>
      </template>
    </VirtualList>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import VirtualList from '@/components/VirtualList.vue'

const notes = ref([
  // 大量数据...
  { id: 1, title: '笔记1', summary: '摘要1' },
  { id: 2, title: '笔记2', summary: '摘要2' },
  // ... 更多数据
])
</script>
```

### 3.2 控制滚动

```vue
<template>
  <div>
    <button @click="scrollToTop">回到顶部</button>
    <button @click="scrollToBottom">滚到底部</button>
    <button @click="scrollToIndex(50)">跳到第50项</button>
    
    <VirtualList
      ref="virtualListRef"
      :list="items"
      :item-height="50"
      :height="400"
    >
      <template #default="{ item }">
        <div class="item">{{ item.name }}</div>
      </template>
    </VirtualList>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import VirtualList from '@/components/VirtualList.vue'

const virtualListRef = ref(null)
const items = ref([...Array(1000)].map((_, i) => ({ id: i, name: `Item ${i}` })))

const scrollToTop = () => {
  virtualListRef.value.scrollToTop()
}

const scrollToBottom = () => {
  virtualListRef.value.scrollToBottom()
}

const scrollToIndex = (index) => {
  virtualListRef.value.scrollToIndex(index)
}
</script>
```

### 3.3 性能对比

| 数据量 | 普通列表渲染时间 | 虚拟列表渲染时间 | 性能提升 |
|--------|-----------------|-----------------|----------|
| 100    | 50ms            | 10ms            | 5x       |
| 1000   | 500ms           | 15ms            | 33x      |
| 10000  | 5000ms          | 20ms            | 250x     |

---

## 四、性能监控

### 4.1 基本用法

```javascript
import { startPerf, endPerf } from '@/utils/performance'

// 测量代码执行时间
startPerf('data-loading')

// ... 执行某些操作 ...
await loadData()

endPerf('data-loading', { itemCount: 100 })
// 输出: ⏱️ [data-loading] 234ms { itemCount: 100 }
```

### 4.2 API 请求监控

```javascript
import { monitorApiCall } from '@/utils/performance'

// 监控 API 调用
const data = await monitorApiCall('/api/notes', () => getNoteListApi())
```

### 4.3 自定义指标

```javascript
import { recordMetric } from '@/utils/performance'

// 记录自定义指标
recordMetric('User Action', 1, { action: 'click', button: 'save' })
recordMetric('Memory Usage', performance.memory?.usedJSHeapSize || 0)
```

### 4.4 Web Vitals 监控

```javascript
import { initPerformanceMonitoring } from '@/utils/performance'

// 在 main.js 中初始化
initPerformanceMonitoring()

// 自动监控:
// - FCP (首次内容绘制)
// - LCP (最大内容绘制)
// - FID (首次输入延迟)
// - CLS (累积布局偏移)
```

### 4.5 页面加载监控

```javascript
import { monitorPageLoad } from '@/utils/performance'

// 自动监控页面加载性能
monitorPageLoad()

// 上报的数据包括:
// - DNS 查询时间
// - TCP 连接时间
// - TTFB (首字节时间)
// - DOM 解析时间
// - 页面完全加载时间
```

---

## 五、综合示例

### 5.1 完整的笔记列表组件

```vue
<template>
  <div class="note-list-page">
    <h2>我的笔记</h2>
    
    <!-- 虚拟滚动列表 -->
    <VirtualList
      ref="listRef"
      :list="notes"
      :item-height="100"
      :height="windowHeight - 200"
    >
      <template #default="{ item }">
        <div class="note-card" @click="viewNote(item.id)">
          <img 
            v-if="item.cover" 
            v-lazy="item.cover" 
            :alt="item.title"
            class="cover-image"
          />
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary }}</p>
        </div>
      </template>
    </VirtualList>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import VirtualList from '@/components/VirtualList.vue'
import { getNoteListApi } from '@/api/note'
import { startPerf, endPerf, monitorApiCall } from '@/utils/performance'

const notes = ref([])
const listRef = ref(null)
const windowHeight = ref(window.innerHeight)

const loadNotes = async () => {
  startPerf('load-notes')
  
  try {
    const response = await monitorApiCall('/note/list', () => 
      getNoteListApi({ page: 1, pageSize: 1000 })
    )
    
    notes.value = response.data.data.records
    endPerf('load-notes', { count: notes.value.length })
  } catch (error) {
    endPerf('load-notes', { error: error.message })
  }
}

onMounted(() => {
  loadNotes()
})
</script>
```

### 5.2 带离线支持的编辑器

```vue
<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { saveNoteDraft, getNoteDraft, deleteNoteDraft } from '@/utils/cache'
import { ElMessage } from 'element-plus'

const noteId = ref(123)
const content = ref('')
const isOnline = ref(navigator.onLine)
let autoSaveTimer = null

// 监听网络状态
window.addEventListener('online', () => {
  isOnline.value = true
  ElMessage.success('网络已恢复')
  syncToServer()
})

window.addEventListener('offline', () => {
  isOnline.value = false
  ElMessage.warning('网络已断开，内容将保存到本地')
})

// 自动保存草稿
watch(content, (newContent) => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  
  autoSaveTimer = setTimeout(async () => {
    const success = await saveNoteDraft(noteId.value, {
      title: '未命名笔记',
      content: newContent
    })
    
    if (success && !isOnline.value) {
      ElMessage.info('已保存到本地')
    }
  }, 1000)
})

// 同步到服务器
const syncToServer = async () => {
  if (!isOnline.value) return
  
  const draft = await getNoteDraft(noteId.value)
  if (draft) {
    try {
      await updateNoteApi(noteId.value, draft)
      await deleteNoteDraft(noteId.value)
      ElMessage.success('已同步到服务器')
    } catch (error) {
      ElMessage.error('同步失败')
    }
  }
}

onMounted(async () => {
  // 尝试从缓存恢复
  const draft = await getNoteDraft(noteId.value)
  if (draft) {
    content.value = draft.content
    ElMessage.info('已恢复未保存的内容')
  }
})

onUnmounted(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
})
</script>
```

---

## 六、注意事项

### 6.1 离线缓存
- ✅ IndexedDB 容量较大（通常 50MB+）
- ✅ 支持存储复杂对象
- ⚠️ 需要处理版本升级
- ⚠️ 定期清理过期数据

### 6.2 图片懒加载
- ✅ 使用 Intersection Observer API，性能好
- ✅ 自动停止观察已加载的图片
- ⚠️ 需要设置合适的 rootMargin
- ⚠️ 注意处理加载失败的情况

### 6.3 虚拟滚动
- ✅ 适合大数据量场景（1000+ 项）
- ✅ 显著减少 DOM 节点数量
- ⚠️ 需要固定每项高度
- ⚠️ 不支持动态高度（需要额外实现）

### 6.4 性能监控
- ✅ 仅在生产环境上报数据
- ✅ 自动监控 Web Vitals
- ⚠️ 需要配置后端接收接口
- ⚠️ 注意隐私和数据安全

---

## 七、最佳实践

1. **离线优先**: 先保存到本地，再同步到服务器
2. **渐进增强**: 核心功能不依赖新特性
3. **错误降级**: 新功能失败时不影响基本功能
4. **性能预算**: 设定性能目标并持续监控
5. **用户体验**: 所有优化都应以提升用户体验为目标