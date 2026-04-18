// src/api/note.ts
import request from '@/utils/request'
import { 
  ApiResponse, 
  NoteVO, 
  CreateNoteDTO, 
  UpdateNoteDTO, 
  UpdateVisibilityDTO, 
  AnnotationVO, 
  CreateAnnotationDTO, 
  UpdateAnnotationDTO,
  NoteVersionHistoryVO,
  FolderVO,
  FolderDTO,
  DeleteFolderDTO,
  NoteQueryDTO,
  CursorPageResult,
  FriendPermissionVO
} from '@/types/api'

// 获取笔记列表
export const getNoteList = (params: NoteQueryDTO) => {
  return request.get<ApiResponse<CursorPageResult<NoteVO>>>('/note/list', { params })
}

// 获取笔记详情
export const getNoteDetail = (noteId: number) => {
  return request.get<ApiResponse<NoteVO>>(`/note/${noteId}`)
}

// 创建笔记
export const createNote = (data: CreateNoteDTO) => {
  return request.post<ApiResponse<any>>('/note', data)
}

// 更新笔记
export const updateNote = (noteId: number, data: UpdateNoteDTO) => {
  return request.put<ApiResponse<any>>(`/note/${noteId}`, data)
}

// 删除笔记
export const deleteNote = (noteId: number) => {
  return request.delete<ApiResponse<any>>(`/note/${noteId}`)
}

// 设置笔记可见性
export const updateVisibility = (noteId: number, data: UpdateVisibilityDTO) => {
  return request.put<ApiResponse<any>>(`/note/${noteId}/visibility`, data)
}

// 获取笔记批注列表
export const getNoteAnnotations = (noteId: number) => {
  return request.get<ApiResponse<AnnotationVO[]>>(`/note/${noteId}/annotations`)
}

// 创建批注
export const createAnnotation = (noteId: number, data: CreateAnnotationDTO) => {
  return request.post<ApiResponse<any>>(`/note/${noteId}/annotations`, data)
}

// 更新批注
export const updateAnnotation = (annotationId: number, data: UpdateAnnotationDTO) => {
  return request.put<ApiResponse<any>>(`/note/annotations/${annotationId}`, data)
}

// 删除批注
export const deleteAnnotation = (annotationId: number) => {
  return request.delete<ApiResponse<any>>(`/note/annotations/${annotationId}`)
}

// 获取版本历史
export const getVersionHistory = (noteId: number) => {
  return request.get<ApiResponse<NoteVersionHistoryVO[]>>(`/note/${noteId}/versions`)
}

// 获取指定版本详情
export const getVersionDetail = (noteId: number, version: number) => {
  return request.get<ApiResponse<NoteVO>>(`/note/${noteId}/versions/${version}`)
}

// 版本回退
export const rollbackToVersion = (noteId: number, version: number) => {
  return request.post<ApiResponse<number>>(`/note/${noteId}/versions/${version}/rollback`)
}

// 获取笔记权限列表
export const getNotePermissions = (noteId: number) => {
  return request.get<ApiResponse<FriendPermissionVO[]>>(`/note/${noteId}/permissions`)
}

// 获取文件夹树
export const getFolderTree = () => {
  return request.get<ApiResponse<FolderVO[]>>('/note/folder/tree')
}

// 获取子文件夹列表
export const getChildFolders = (parentId?: number | null) => {
  return request.get<ApiResponse<FolderVO[]>>('/note/folder/children', { 
    params: { parentId } 
  })
}

// 创建文件夹
export const createFolder = (data: FolderDTO) => {
  return request.post<ApiResponse<any>>('/note/folder', data)
}

// 重命名文件夹
export const renameFolder = (folderId: number, data: FolderDTO) => {
  return request.put<ApiResponse<any>>(`/note/folder/${folderId}`, data)
}

// 删除文件夹
export const deleteFolder = (folderId: number, data?: DeleteFolderDTO) => {
  return request.delete<ApiResponse<any>>(`/note/folder/${folderId}`, { data })
}

// 获取回收站笔记列表
export const getRecycleBinNotes = () => {
  return request.get<ApiResponse<any>>('/recycle-bin/notes')
}

// 获取回收站文件夹列表
export const getRecycleBinFolders = () => {
  return request.get<ApiResponse<any>>('/recycle-bin/folders')
}

// 还原笔记
export const restoreNote = (noteId: number) => {
  return request.post<ApiResponse<any>>(`/recycle-bin/notes/${noteId}/restore`)
}

// 还原文件夹
export const restoreFolder = (folderId: number) => {
  return request.post<ApiResponse<any>>(`/recycle-bin/folders/${folderId}/restore`)
}

// 彻底删除笔记
export const permanentlyDeleteNote = (noteId: number) => {
  return request.delete<ApiResponse<any>>(`/recycle-bin/notes/${noteId}`)
}

// 彻底删除文件夹
export const permanentlyDeleteFolder = (folderId: number) => {
  return request.delete<ApiResponse<any>>(`/recycle-bin/folders/${folderId}`)
}

// 清空回收站
export const emptyRecycleBin = () => {
  return request.delete<ApiResponse<any>>('/recycle-bin/empty')
}

// 上传笔记图片
export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<any>>('/note/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 导出为PDF
export const exportAsPdf = (noteId: number) => {
  return request.get<Blob>(`/note/${noteId}/export/pdf`, {
    responseType: 'blob'
  })
}

// 导出为Markdown
export const exportAsMarkdown = (noteId: number) => {
  return request.get<Blob>(`/note/${noteId}/export/md`, {
    responseType: 'blob'
  })
}

// AI分析笔记
export const analyzeNote = (noteId: number, forceRefresh = false) => {
  return request.post<ApiResponse<any>>(`/note/${noteId}/ai-analyze`, { noteId, forceRefresh })
}

// 获取最常看的3篇笔记
export const getTop3FrequentNotes = () => {
  return request.get<ApiResponse<NoteVO[]>>('/note/top3-frequent')
}

// 获取最近查看的笔记
export const getRecentViewedNotes = (limit = 10) => {
  return request.get<ApiResponse<NoteVO[]>>('/note/recent', { params: { limit } })
}