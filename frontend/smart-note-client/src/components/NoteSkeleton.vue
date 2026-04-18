<!-- src/components/NoteSkeleton.vue -->
<template>
  <div class="note-skeleton">
    <div class="skeleton-header">
      <div class="skeleton-title"></div>
      <div class="skeleton-meta"></div>
    </div>
    <div class="skeleton-content">
      <div class="skeleton-line"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line short"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line medium"></div>
    </div>
    <div class="skeleton-actions">
      <div class="skeleton-button"></div>
      <div class="skeleton-button small"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 笔记骨架屏组件
</script>

<style scoped>
.note-skeleton {
  padding: 20px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  margin-bottom: 15px;
  animation: skeleton-loading 1.5s infinite ease-in-out alternate;
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.skeleton-title {
  height: 24px;
  width: 200px;
  background-color: #e9ecef;
  border-radius: 4px;
}

.skeleton-meta {
  height: 20px;
  width: 100px;
  background-color: #e9ecef;
  border-radius: 4px;
}

.skeleton-content {
  margin-bottom: 15px;
}

.skeleton-line {
  height: 16px;
  background-color: #e9ecef;
  border-radius: 4px;
  margin-bottom: 10px;
}

.skeleton-line.short {
  width: 60%;
}

.skeleton-line.medium {
  width: 80%;
}

.skeleton-actions {
  display: flex;
  gap: 10px;
}

.skeleton-button {
  height: 32px;
  width: 80px;
  background-color: #e9ecef;
  border-radius: 4px;
}

.skeleton-button.small {
  width: 60px;
}

@keyframes skeleton-loading {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0.5;
  }
}
</style>