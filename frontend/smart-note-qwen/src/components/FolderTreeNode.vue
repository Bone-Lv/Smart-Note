<template>
  <div class="folder-tree-node">
    <!-- TODO: 拖拽功能需要后端支持，暂时禁用 -->
    <!-- draggable="true"
    @dragstart="$emit('dragstart', $event, folder)"
    @dragover.prevent
    @drop="$emit('drop', $event, folder)" -->
    
    <div 
      class="folder-node-content"
      :class="{ active: currentFolderId === folder.id }"
      @click="$emit('select', folder.id)"
    >
      <!-- 展开/折叠图标 -->
      <i 
        v-if="hasChildren"
        class="fas expand-icon"
        :class="isExpanded ? 'fa-chevron-down' : 'fa-chevron-right'"
        @click.stop="toggleExpand"
      ></i>
      <span v-else class="expand-placeholder"></span>
      
      <!-- 文件夹图标和名称 -->
      <i class="fas fa-folder folder-icon"></i>
      <span class="folder-name">{{ folder.name }}</span>
      
      <!-- 操作按钮 -->
      <div class="folder-actions" @click.stop>
        <button @click="$emit('edit', folder)" class="action-btn" title="重命名">
          <i class="fas fa-edit"></i>
        </button>
        <button @click="$emit('delete', folder)" class="action-btn danger" title="删除">
          <i class="fas fa-trash-alt"></i>
        </button>
      </div>
    </div>
    
    <!-- 子文件夹（递归渲染） -->
    <div v-if="hasChildren && isExpanded" class="folder-children">
      <FolderTreeNode
        v-for="child in folder.children"
        :key="child.id"
        :folder="child"
        :current-folder-id="currentFolderId"
        :search-keyword="searchKeyword"
        @select="$emit('select', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  folder: {
    type: Object,
    required: true
  },
  currentFolderId: {
    type: [String, Number],
    default: null
  },
  searchKeyword: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['select', 'edit', 'delete']);

// 是否有子文件夹
const hasChildren = computed(() => {
  return props.folder.children && props.folder.children.length > 0;
});

// 展开状态
const isExpanded = ref(false);

// 切换展开/折叠
const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
};
</script>

<style scoped>
.folder-tree-node {
  margin-bottom: 4px;
}

.folder-node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.folder-node-content:hover {
  background: #f5f7fa;
}

.folder-node-content.active {
  background: #ecf5ff;
  color: #409eff;
}

.expand-icon, .expand-placeholder {
  width: 16px;
  text-align: center;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.expand-placeholder {
  display: inline-block;
}

.expand-icon:hover {
  color: #409eff;
}

.folder-icon {
  font-size: 16px;
  color: #ffa500;
}

.folder-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.folder-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.folder-node-content:hover .folder-actions {
  opacity: 1;
}

.action-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: all 0.3s;
}

.action-btn:hover {
  background: #f0f0f0;
  color: #409eff;
}

.action-btn.danger:hover {
  background: #ffebee;
  color: #f56c6c;
}

.folder-children {
  margin-left: 24px;
  border-left: 1px dashed #ddd;
  padding-left: 8px;
}
</style>
