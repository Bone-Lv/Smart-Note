// src/hooks/useWebSocket.ts
import { ref, onMounted, onUnmounted } from 'vue'
import { userStore } from '@/stores/userStore'
import { 
  WebSocketMessage, 
  WebSocketMessageType, 
  NoteEditRequestData, 
  NoteContentUpdateData,
  MarkReadData,
  NoteViewData
} from '@/types/websocket'

interface WebSocketHookReturn {
  isConnected: boolean
  sendMessage: (message: WebSocketMessage) => void
  connect: () => void
  disconnect: () => void
}

export function useWebSocket(): WebSocketHookReturn {
  const ws = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const reconnectTimer = ref<number | null>(null)
  const maxReconnectAttempts = 5
  const reconnectAttempts = ref(0)
  const heartbeatTimer = ref<number | null>(null)
  const pingTimer = ref<number | null>(null)

  const connect = () => {
    if (isConnected.value) return
    
    const store = userStore()
    const token = store.getToken()
    
    if (!token) {
      console.error('No token available for WebSocket connection')
      return
    }

    try {
      // 在实际项目中，这里应该是ws://或wss://协议的地址
      // 这里使用模拟连接，实际项目中替换为真实地址
      const wsUrl = `ws://localhost:8080/ws?token=${token}`
      ws.value = new WebSocket(wsUrl)
      
      ws.value.onopen = () => {
        console.log('WebSocket connected')
        isConnected.value = true
        reconnectAttempts.value = 0
        
        // 启动心跳
        startHeartbeat()
      }
      
      ws.value.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          handleMessage(message)
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error)
        }
      }
      
      ws.value.onclose = () => {
        console.log('WebSocket disconnected')
        isConnected.value = false
        stopHeartbeat()
        
        // 尝试重连
        if (reconnectAttempts.value < maxReconnectAttempts) {
          reconnectTimer.value = setTimeout(() => {
            reconnectAttempts.value++
            connect()
          }, 3000) as unknown as number
        }
      }
      
      ws.value.onerror = (error) => {
        console.error('WebSocket error:', error)
        isConnected.value = false
      }
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
    }
  }

  const disconnect = () => {
    if (ws.value) {
      ws.value.close()
    }
    isConnected.value = false
    stopHeartbeat()
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
  }

  const sendMessage = (message: WebSocketMessage) => {
    if (ws.value && isConnected.value) {
      ws.value.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket not connected, message not sent:', message)
    }
  }

  const startHeartbeat = () => {
    // 发送心跳
    heartbeatTimer.value = setInterval(() => {
      if (isConnected.value) {
        sendMessage({
          type: WebSocketMessageType.PING,
          data: {},
          timestamp: Date.now()
        })
      }
    }, 30000) // 每30秒发送一次心跳

    // 设置超时检测
    pingTimer.value = setTimeout(() => {
      if (isConnected.value) {
        // 如果长时间没有收到PONG，主动断开重连
        disconnect()
        connect()
      }
    }, 40000) // 40秒超时
  }

  const stopHeartbeat = () => {
    if (heartbeatTimer.value) {
      clearInterval(heartbeatTimer.value)
      heartbeatTimer.value = null
    }
    if (pingTimer.value) {
      clearTimeout(pingTimer.value)
      pingTimer.value = null
    }
  }

  const handleMessage = (message: WebSocketMessage) => {
    switch (message.type) {
      case WebSocketMessageType.CONNECTED:
        console.log('WebSocket connected successfully')
        break
      case WebSocketMessageType.PONG:
        // 收到心跳响应，重置超时检测
        if (pingTimer.value) {
          clearTimeout(pingTimer.value)
          pingTimer.value = setTimeout(() => {
            if (isConnected.value) {
              disconnect()
              connect()
            }
          }, 40000)
        }
        break
      case WebSocketMessageType.OFFLINE_MESSAGE_COUNT:
        console.log('Offline message count:', message.data.count)
        break
      case WebSocketMessageType.EDIT_LOCK_GRANTED:
        console.log('Edit lock granted for note:', message.data.noteId)
        // 这里可以触发UI更新，允许编辑
        break
      case WebSocketMessageType.EDIT_LOCK_DENIED:
        console.log('Edit lock denied for note:', message.data.noteId)
        // 提示用户无法编辑
        break
      case WebSocketMessageType.EDIT_LOCK_RELEASED:
        console.log('Edit lock released for note:', message.data.noteId)
        // 释放编辑锁，其他用户可以编辑了
        break
      case WebSocketMessageType.NOTE_CONTENT_UPDATED:
        console.log('Note content updated:', message.data)
        // 更新笔记内容
        break
      default:
        console.log('Received message:', message)
    }
  }

  // 请求笔记编辑锁
  const requestEditLock = (noteId: number) => {
    sendMessage({
      type: WebSocketMessageType.NOTE_EDIT_REQUEST,
      data: { noteId } as NoteEditRequestData,
      timestamp: Date.now()
    })
  }

  // 释放笔记编辑锁
  const releaseEditLock = (noteId: number) => {
    sendMessage({
      type: WebSocketMessageType.NOTE_EDIT_RELEASE,
      data: { noteId } as NoteEditRequestData,
      timestamp: Date.now()
    })
  }

  // 更新笔记内容
  const updateNoteContent = (noteId: number, content: string, title: string, version: number) => {
    sendMessage({
      type: WebSocketMessageType.NOTE_CONTENT_UPDATE,
      data: { noteId, content, title, version } as NoteContentUpdateData,
      timestamp: Date.now()
    })
  }

  // 标记消息已读
  const markMessageAsRead = (messageId: number) => {
    sendMessage({
      type: WebSocketMessageType.MARK_READ,
      data: { messageId } as MarkReadData,
      timestamp: Date.now()
    })
  }

  // 开始查看笔记
  const startNoteView = (noteId: number) => {
    sendMessage({
      type: WebSocketMessageType.NOTE_VIEW_START,
      data: { noteId } as NoteViewData,
      timestamp: Date.now()
    })
  }

  // 结束查看笔记
  const endNoteView = (noteId: number) => {
    sendMessage({
      type: WebSocketMessageType.NOTE_VIEW_END,
      data: { noteId } as NoteViewData,
      timestamp: Date.now()
    })
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
    }
  })

  return {
    isConnected,
    sendMessage,
    connect,
    disconnect
  }
}