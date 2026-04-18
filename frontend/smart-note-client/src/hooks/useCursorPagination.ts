// src/hooks/useCursorPagination.ts
import { ref, computed } from 'vue'

interface CursorPaginationOptions<T> {
  fetchData: (cursor?: number | null, limit?: number) => Promise<{ data: T[], nextCursor: number | null, hasNext: boolean }>
  initialLimit?: number
}

interface CursorPaginationReturn<T> {
  items: T[]
  loading: boolean
  hasMore: boolean
  loadMore: () => Promise<void>
  reset: () => void
  refresh: () => Promise<void>
}

export function useCursorPagination<T>({
  fetchData,
  initialLimit = 20
}: CursorPaginationOptions<T>): CursorPaginationReturn<T> {
  const items = ref<T[]>([])
  const loading = ref(false)
  const nextCursor = ref<number | null>(null)
  const hasMore = computed(() => nextCursor.value !== null)

  const loadMore = async () => {
    if (loading.value || !hasMore.value) return

    loading.value = true
    try {
      const result = await fetchData(nextCursor.value, initialLimit)
      items.value = [...items.value, ...result.data]
      nextCursor.value = result.nextCursor
    } catch (error) {
      console.error('Error loading more items:', error)
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    items.value = []
    nextCursor.value = null
  }

  const refresh = async () => {
    reset()
    loading.value = true
    try {
      const result = await fetchData(null, initialLimit)
      items.value = result.data
      nextCursor.value = result.nextCursor
    } catch (error) {
      console.error('Error refreshing items:', error)
    } finally {
      loading.value = false
    }
  }

  return {
    items,
    loading,
    hasMore,
    loadMore,
    reset,
    refresh
  }
}