import { ref } from 'vue'
import type { AnnotationVO } from '@/types/note'

export function useNoteAnnotations() {
  const annotations = ref<AnnotationVO[]>([])

  const calculateValidPositions = (content: string) => {
    return annotations.value.map(anno => {
      // 1. 检查原位置是否有效
      const savedText = content.substring(anno.startPosition, anno.endPosition)
      if (savedText === anno.targetContent) {
        return { ...anno, validStart: anno.startPosition, validEnd: anno.endPosition }
      }
      // 2. 全文搜索定位
      const newIndex = content.indexOf(anno.targetContent)
      if (newIndex !== -1) {
        return { ...anno, validStart: newIndex, validEnd: newIndex + anno.targetContent.length }
      }
      return null // 定位失败
    }).filter(Boolean)
  }

  const setAnnotations = (data: AnnotationVO[]) => { annotations.value = data }

  return { annotations, setAnnotations, calculateValidPositions }
}