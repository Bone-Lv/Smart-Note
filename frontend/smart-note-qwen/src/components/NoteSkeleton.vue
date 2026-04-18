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
      <div class="skeleton-line long"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line medium"></div>
      <div class="skeleton-line"></div>
    </div>
    <div class="skeleton-actions">
      <div class="skeleton-action"></div>
      <div class="skeleton-action"></div>
      <div class="skeleton-action"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NoteSkeleton'
};
</script>

<style scoped>
.note-skeleton {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  animation: skeleton-loading 1.5s infinite alternate;
}

.skeleton-header {
  margin-bottom: 16px;
}

.skeleton-title {
  width: 60%;
  height: 24px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-meta {
  width: 30%;
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  border-radius: 4px;
}

.skeleton-content {
  margin-bottom: 16px;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-line.long {
  width: 90%;
}

.skeleton-line.medium {
  width: 70%;
}

.skeleton-line.short {
  width: 40%;
}

.skeleton-actions {
  display: flex;
  gap: 12px;
}

.skeleton-action {
  width: 60px;
  height: 32px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  border-radius: 4px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>