<template>
  <div style="padding: 10px 0">
    <div class="skeleton" style="height: 20px; width: 80%; background: #f2f2f2; border-radius: 4px; margin: 8px 0;"></div>
    <div class="skeleton" style="height: 20px; width: 60%; background: #f2f2f2; border-radius: 4px; margin: 8px 0;"></div>
  </div>
</template>

<style scoped>
.skeleton {
  animation: loading 1s infinite alternate;
}
@keyframes loading {
  from { opacity: 0.5; }
  to { opacity: 1; }
}
</style>