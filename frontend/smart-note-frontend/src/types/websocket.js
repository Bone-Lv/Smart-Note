export const WS_TYPE = {
  // 客户端 -> 服务端
  PING: "ping",
  MARK_READ: "mark_read",
  NOTE_EDIT_REQUEST: "note_edit_request",
  NOTE_EDIT_RELEASE: "note_edit_release",
  NOTE_CONTENT_UPDATE: "note_content_update",
  NOTE_VIEW_START: "note_view_start",
  NOTE_VIEW_END: "note_view_end",

  // 服务端 -> 客户端
  CONNECTED: "connected",
  PONG: "pong",
  OFFLINE_MESSAGE_COUNT: "offline_message_count",
  EDIT_LOCK_GRANTED: "edit_lock_granted",
  EDIT_LOCK_DENIED: "edit_lock_denied",
  EDIT_LOCK_RELEASED: "edit_lock_released",
  NOTE_CONTENT_UPDATED: "note_content_updated"
}