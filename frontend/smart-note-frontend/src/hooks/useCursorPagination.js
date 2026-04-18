import { ref, reactive } from 'vue';

export const useCursorPagination = (fetchFunction, pageSize = 20) => {
  const state = reactive({
    items: [],
    hasNext: true,
    nextCursor: null,
    loading: false,
    hasMore: true
  });

  const loadMore = async (additionalParams = {}) => {
    if (!state.hasMore || state.loading) return;

    state.loading = true;

    try {
      const params = {
        pageSize,
        cursor: state.nextCursor || undefined,
        ...additionalParams
      };

      const response = await fetchFunction(params);

      if (response.data && response.data.hasNext) {
        state.items = [...state.items, ...response.data.records];
        state.nextCursor = response.data.nextCursor;
        state.hasNext = response.data.hasNext;
      } else {
        state.items = [...state.items, ...(response.data?.records || [])];
        state.hasMore = false;
      }
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      state.loading = false;
    }
  };

  const reset = () => {
    state.items = [];
    state.nextCursor = null;
    state.hasNext = true;
    state.hasMore = true;
  };

  const refresh = async (additionalParams = {}) => {
    reset();
    await loadMore(additionalParams);
  };

  return {
    state,
    loadMore,
    reset,
    refresh
  };
};