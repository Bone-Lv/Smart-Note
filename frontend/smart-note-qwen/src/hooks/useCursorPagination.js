import { ref, reactive } from 'vue';

export const useCursorPagination = (fetchFunction) => {
  const loading = ref(false);
  const hasMore = ref(true);
  const nextCursor = ref(null);
  
  const items = ref([]);

  const reset = () => {
    items.value = [];
    nextCursor.value = null;
    hasMore.value = true;
  };

  const loadMore = async (additionalParams = {}) => {
    if (loading.value || !hasMore.value) return;

    loading.value = true;

    try {
      const params = {
        ...additionalParams,
        cursor: nextCursor.value,
        pageSize: 20
      };

      const response = await fetchFunction(params);
      const result = response.data.data;
      
      if (result && result.records) {
        items.value = [...items.value, ...result.records];
        nextCursor.value = result.nextCursor;
        hasMore.value = result.hasNext;
      }
    } catch (error) {
      console.error('Load more error:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const loadFirstPage = async (additionalParams = {}) => {
    reset();
    await loadMore(additionalParams);
  };

  return {
    items,
    loading,
    hasMore,
    nextCursor,
    reset,
    loadMore,
    loadFirstPage
  };
};