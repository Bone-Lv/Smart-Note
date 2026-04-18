import { ref, reactive } from 'vue';

// WebSocket消息类型枚举
export const WS_MESSAGE_TYPES = {
  // 客户端 → 服务端
  PING: 'ping',                    // 心跳请求
  MARK_READ: 'mark_read',          // 标记已读
  NOTE_EDIT_REQUEST: 'note_edit_request',     // 请求编辑锁
  NOTE_EDIT_RELEASE: 'note_edit_release',     // 释放编辑锁
  NOTE_CONTENT_UPDATE: 'note_content_update', // 笔记内容更新
  NOTE_VIEW_START: 'note_view_start',         // 开始查看笔记
  NOTE_VIEW_END: 'note_view_end',             // 停止查看笔记
  
  // 服务端 → 客户端
  CONNECTED: 'connected',                      // 连接成功
  PONG: 'pong',                                // 心跳响应
  OFFLINE_MESSAGE_COUNT: 'offline_message_count', // 离线消息数
  EDIT_LOCK_GRANTED: 'edit_lock_granted',      // 编辑锁授予
  EDIT_LOCK_DENIED: 'edit_lock_denied',        // 编辑锁拒绝
  EDIT_LOCK_RELEASED: 'edit_lock_released',    // 编辑锁释放
  NOTE_CONTENT_UPDATED: 'note_content_updated',// 笔记内容更新
  PRIVATE_MESSAGE: 'private_message',          // 私聊消息
  GROUP_MESSAGE: 'group_message',              // 群聊消息
  NOTE_PERMISSION: 'note_permission',          // 笔记权限通知
  FRIEND_ONLINE: 'friend_online',              // 好友上线通知
  FRIEND_OFFLINE: 'friend_offline',            // 好友下线通知
  GROUP_DISBANDED: 'group_disbanded'           // 群聊解散通知
};

// 默认WebSocket地址
const DEFAULT_WS_URL = 'ws://localhost:8080/ws';

export const useWebSocket = () => {
  const ws = ref(null);
  const isConnected = ref(false);
  const reconnectAttempts = ref(0);
  const maxReconnectAttempts = 5;
  const reconnectInterval = 3000;
  
  let heartbeatTimer = null;
  let reconnectTimer = null;

  const messageHandlers = reactive({});

  /**
   * 连接WebSocket
   * @param {string} url - WebSocket地址，默认 ws://localhost:8080/ws
   */
  const connect = (url = DEFAULT_WS_URL) => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      console.warn('WebSocket already connected');
      return;
    }

    // ⚠️ WebSocket握手时，浏览器会自动携带HttpOnly Cookie
    // 无需在URL中传递token参数
    ws.value = new WebSocket(url);

    ws.value.onopen = () => {
      console.log('✅ WebSocket连接成功');
      console.log('🔗 WebSocket地址:', ws.value.url);
      console.log('📡 WebSocket实例:', ws.value);
      isConnected.value = true;
      reconnectAttempts.value = 0;
      
      // 启动心跳
      startHeartbeat();
    };

    ws.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        console.log('📨 WebSocket收到原始消息:');
        console.log('  完整消息:', message);
        console.log('  消息类型:', message.type);
        handleMessage(message);
      } catch (error) {
        console.error('❌ 解析WebSocket消息失败:', error);
        console.error('  原始数据:', event.data);
      }
    };

    ws.value.onclose = () => {
      console.log('⚠️ WebSocket连接关闭');
      isConnected.value = false;
      
      // 停止心跳
      stopHeartbeat();
      
      // 自动重连
      scheduleReconnect(url);
    };

    ws.value.onerror = (error) => {
      console.error('❌ WebSocket错误:', error);
    };
  };

  /**
   * 发送消息
   * @param {string} type - 消息类型
   * @param {object} data - 消息数据
   */
  const send = (type, data = {}) => {
    console.log('📤 准备发送消息:');
    console.log('  类型:', type);
    console.log('  数据:', data);
    console.log('  WebSocket连接状态:', isConnected.value);
    console.log('  WebSocket readyState:', ws.value?.readyState);
    
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket未连接，无法发送消息');
      console.log('  ws.value:', ws.value);
      console.log('  readyState:', ws.value?.readyState);
      return false;
    }

    try {
      const message = { type, ...data };
      console.log('  发送完整消息:', JSON.stringify(message));
      ws.value.send(JSON.stringify(message));
      console.log('  ✅ 消息发送成功');
      return true;
    } catch (error) {
      console.error('❌ 发送消息失败:', error);
      return false;
    }
  };

  /**
   * 注册消息处理器
   * @param {string} type - 消息类型
   * @param {function} handler - 处理函数
   */
  const on = (type, handler) => {
    console.log(`📝 注册消息处理器: ${type}`);
    console.log('  处理器函数:', handler.name || '匿名函数');
    if (!messageHandlers[type]) {
      messageHandlers[type] = [];
    }
    messageHandlers[type].push(handler);
    console.log(`  ✅ ${type} 当前有 ${messageHandlers[type].length} 个处理器`);
  };

  /**
   * 取消注册消息处理器
   * @param {string} type - 消息类型
   * @param {function} handler - 处理函数
   */
  const off = (type, handler) => {
    console.log(`🗑️ 取消注册消息处理器: ${type}`);
    if (messageHandlers[type]) {
      const index = messageHandlers[type].indexOf(handler);
      if (index > -1) {
        messageHandlers[type].splice(index, 1);
        console.log(`  ✅ 已移除，当前有 ${messageHandlers[type].length} 个处理器`);
      } else {
        console.log('  ⚠️ 处理器不存在，无需移除');
      }
    } else {
      console.log('  ⚠️ 该类型没有注册的处理器');
    }
  };

  /**
   * 处理接收到的消息
   * @param {object} message - 消息对象
   */
  const handleMessage = (message) => {
    const { type } = message;
    console.log('🔍 handleMessage 开始处理消息:');
    console.log('  消息类型:', type);
    console.log('  消息内容:', message);
    console.log('  当前注册的处理器类型:', Object.keys(messageHandlers));
    
    // 调用注册的处理器
    if (messageHandlers[type]) {
      console.log(`✅ 找到 ${type} 的处理器，数量:`, messageHandlers[type].length);
      messageHandlers[type].forEach((handler, index) => {
        console.log(`  执行处理器 #${index + 1}:`, handler.name || '匿名函数');
        try {
          handler(message);
          console.log(`  ✅ 处理器 #${index + 1} 执行成功`);
        } catch (error) {
          console.error(`❌ 处理器 #${index + 1} 执行失败:`, error);
        }
      });
    } else {
      console.warn(`⚠️ 没有找到 ${type} 的处理器`);
      console.log('  可用的处理器:', Object.keys(messageHandlers));
    }
  };

  /**
   * 启动心跳机制
   */
  const startHeartbeat = () => {
    stopHeartbeat(); // 先清除旧的心跳
    
    heartbeatTimer = setInterval(() => {
      if (isConnected.value) {
        send(WS_MESSAGE_TYPES.PING);
      }
    }, 30000); // 每30秒发送一次心跳
  };

  /**
   * 停止心跳机制
   */
  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
  };

  /**
   * 安排重连
   * @param {string} url - WebSocket地址
   */
  const scheduleReconnect = (url) => {
    if (reconnectTimer) return;
    
    if (reconnectAttempts.value < maxReconnectAttempts) {
      reconnectTimer = setTimeout(() => {
        reconnectAttempts.value++;
        console.log(`🔄 尝试重新连接... (${reconnectAttempts.value}/${maxReconnectAttempts})`);
        reconnectTimer = null;
        connect(url);
      }, reconnectInterval);
    } else {
      console.error('❌ 达到最大重连次数，停止重连');
    }
  };

  /**
   * 断开连接
   */
  const disconnect = () => {
    stopHeartbeat();
    
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    
    if (ws.value) {
      ws.value.close();
      ws.value = null;
    }
    
    isConnected.value = false;
    reconnectAttempts.value = 0;
  };

  /**
   * 重置重连计数
   */
  const resetReconnectAttempts = () => {
    reconnectAttempts.value = 0;
  };

  // ========== 便捷方法 ==========

  /**
   * 发送心跳
   */
  const sendPing = () => {
    return send(WS_MESSAGE_TYPES.PING);
  };

  /**
   * 标记消息已读
   * @param {object} params - 参数对象
   * @param {number} [params.friendUserId] - 好友ID（私聊批量标记）
   * @param {number} [params.upToMessageId] - 标记到此ID为止的消息（私聊批量标记）
   * @param {number} [params.messageId] - 单个消息ID（私聊单条标记）
   * @param {number} [params.groupId] - 群ID（群聊标记已读）
   */
  const markRead = (params) => {
    // 根据参数类型决定发送的数据结构
    if (params.groupId) {
      // 群聊标记已读：只需要 groupId
      return send(WS_MESSAGE_TYPES.MARK_READ, {
        groupId: params.groupId
      });
    } else if (params.friendUserId && params.upToMessageId) {
      // 私聊批量标记
      return send(WS_MESSAGE_TYPES.MARK_READ, {
        friendUserId: params.friendUserId,
        upToMessageId: params.upToMessageId
      });
    } else if (params.messageId) {
      // 私聊单条标记
      return send(WS_MESSAGE_TYPES.MARK_READ, {
        messageId: params.messageId
      });
    } else {
      console.warn('⚠️ markRead 参数不完整:', params);
      return Promise.reject(new Error('标记已读参数不完整'));
    }
  };

  /**
   * 请求笔记编辑锁
   * @param {number} noteId - 笔记ID
   */
  const requestEditLock = (noteId) => {
    return send(WS_MESSAGE_TYPES.NOTE_EDIT_REQUEST, { noteId });
  };

  /**
   * 释放笔记编辑锁
   * @param {number} noteId - 笔记ID
   */
  const releaseEditLock = (noteId) => {
    return send(WS_MESSAGE_TYPES.NOTE_EDIT_RELEASE, { noteId });
  };

  /**
   * 发送笔记内容更新
   * @param {object} params - 参数对象
   * @param {number} params.noteId - 笔记ID
   * @param {string} params.content - 笔记内容
   * @param {number} params.version - 版本号
   */
  const updateNoteContent = (params) => {
    return send(WS_MESSAGE_TYPES.NOTE_CONTENT_UPDATE, params);
  };

  /**
   * 开始查看笔记
   * @param {number} noteId - 笔记ID
   */
  const startViewNote = (noteId) => {
    return send(WS_MESSAGE_TYPES.NOTE_VIEW_START, { noteId });
  };

  /**
   * 停止查看笔记
   * @param {number} noteId - 笔记ID
   */
  const endViewNote = (noteId) => {
    return send(WS_MESSAGE_TYPES.NOTE_VIEW_END, { noteId });
  };

  // 监听PONG响应（保持连接活跃）
  on(WS_MESSAGE_TYPES.PONG, () => {
    // 服务器响应心跳，继续保持连接
  });

  return {
    ws,
    isConnected,
    reconnectAttempts,
    connect,
    disconnect,
    send,
    on,
    off,
    handleMessage,
    startHeartbeat,
    stopHeartbeat,
    resetReconnectAttempts,
    
    // 便捷方法
    sendPing,
    markRead,
    requestEditLock,
    releaseEditLock,
    updateNoteContent,
    startViewNote,
    endViewNote,
    
    // 消息类型常量
    WS_MESSAGE_TYPES
  };
};