export interface NoteVO {
  id: number;
  title: string;
  content: string;
  folderId: number;
  updateTime: string;
}

export interface AnnotationVO {
  id: number;
  content: string;
  targetContent: string;
  startPosition: number;
  endPosition: number;
  username: string;
}