import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * 全局Loading状态管理
 * 使用计数器支持并发请求，避免loading闪烁
 */
export const useLoadingStore = defineStore('loading', () => {
  // Loading计数器
  const loadingCount = ref(0);
  
  // 计算属性：是否正在loading
  const isLoading = computed(() => loadingCount.value > 0);

  /**
   * 开始loading（计数器+1）
   */
  const startLoading = () => {
    loadingCount.value++;
  };

  /**
   * 结束loading（计数器-1）
   */
  const endLoading = () => {
    if (loadingCount.value > 0) {
      loadingCount.value--;
    }
  };

  /**
   * 强制重置loading状态
   */
  const resetLoading = () => {
    loadingCount.value = 0;
  };

  return {
    loadingCount,
    isLoading,
    startLoading,
    endLoading,
    resetLoading
  };
});
