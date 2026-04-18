import { ElMessage, ElNotification } from 'element-plus';

/**
 * WebSocket 服务类
 * 用于实现实时笔记协作和即时通讯功能
 * 使用 HttpOnly Cookie 认证，无需手动传递 token
 */
class WebSocketService {
  constructor() {
    this.ws = null;
    this.reconnectTimer = null;
    this.heartbeatTimer = null;
    this.messageHandlers = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 10;
    this.isConnected = false;
  }

  /**
   * 连接 WebSocket
   * 浏览器会自动携带 HttpOnly Cookie 进行认证
   */
  connect() {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      console.log('WebSocket 已经连接或正在连接中');
      return;
    }

    const wsUrl = 'ws://localhost:8080/ws';
    console.log('🔌 尝试连接 WebSocket:', wsUrl);

    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('✅ WebSocket 连接成功');
      this.isConnected = true;
      this.reconnectAttempts = 0;
      this.startHeartbeat();
      this.triggerHandler('connected', { type: 'connected' });
    };

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        // 🔍 调试：打印所有收到的消息
        if (message.type === 'friend_online' || message.type === 'friend_offline') {
          console.log('📡 WebSocket 原始消息 (event.data):', event.data);
          console.log('📦 解析后的消息对象:', message);
        }
        
        this.handleMessage(message);
      } catch (error) {
        console.error('❌ 解析 WebSocket 消息失败:', error);
      }
    };

    this.ws.onclose = (event) => {
      console.log('⚠️ WebSocket 连接关闭:', event.code, event.reason);
      this.isConnected = false;
      this.stopHeartbeat();
      this.triggerHandler('disconnected', { type: 'disconnected', code: event.code });
      this.reconnect();
    };

    this.ws.onerror = (error) => {
      console.error('❌ WebSocket 错误:', error);
      this.triggerHandler('error', { type: 'error', error });
    };
  }

  /**
   * 发送消息
   * @param {string} type - 消息类型
   * @param {object} data - 消息数据
   */
  send(type, data = {}) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket 未连接，无法发送消息:', type);
      return false;
    }

    try {
      const message = JSON.stringify({ type, ...data });
      this.ws.send(message);
      return true;
    } catch (error) {
      console.error('❌ 发送消息失败:', error);
      return false;
    }
  }

  /**
   * 注册消息处理器
   * @param {string} type - 消息类型
   * @param {function} handler - 处理函数
   */
  on(type, handler) {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, []);
    }
    this.messageHandlers.get(type).push(handler);
  }

  /**
   * 移除消息处理器
   * @param {string} type - 消息类型
   * @param {function} handler - 处理函数（可选，不传则移除所有）
   */
  off(type, handler) {
    if (!this.messageHandlers.has(type)) return;

    if (handler) {
      const handlers = this.messageHandlers.get(type);
      const index = handlers.indexOf(handler);
      if (index !== -1) {
        handlers.splice(index, 1);
      }
    } else {
      this.messageHandlers.delete(type);
    }
  }

  /**
   * 处理接收到的消息
   * @param {object} message - 消息对象
   */
  handleMessage(message) {
    const handlers = this.messageHandlers.get(message.type);
    if (handlers && handlers.length > 0) {
      handlers.forEach(handler => {
        try {
          handler(message);
        } catch (error) {
          console.error(`❌ 消息处理器执行失败 (${message.type}):`, error);
        }
      });
    } else {
      // 没有注册处理器的消息类型，默认不处理
      console.debug('📩 收到未处理的消息类型:', message.type);
    }
  }

  /**
   * 触发处理器
   * @private
   */
  triggerHandler(type, message) {
    const handlers = this.messageHandlers.get(type);
    if (handlers) {
      handlers.forEach(handler => handler(message));
    }
  }

  /**
   * 开始心跳
   * 每 30 秒发送一次 ping
   */
  startHeartbeat() {
    this.stopHeartbeat(); // 先停止已有的心跳
    this.heartbeatTimer = setInterval(() => {
      this.send('ping');
    }, 30000); // 30 秒
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * 重连机制
   * 使用指数退避策略，最多重连 10 次
   */
  reconnect() {
    if (this.reconnectTimer) {
      return; // 已经在重连中
    }

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ WebSocket 重连失败次数过多，停止重连');
      ElMessage.error('WebSocket 连接失败，请刷新页面重试');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 30000); // 指数退避，最大 30 秒

    console.log(`🔄 ${delay / 1000}秒后尝试第 ${this.reconnectAttempts} 次重连...`);

    this.reconnectTimer = setTimeout(() => {
      console.log('🔌 尝试重新连接 WebSocket...');
      this.connect();
      this.reconnectTimer = null;
    }, delay);
  }

  /**
   * 断开连接
   * 通常在用户退出登录时调用
   */
  disconnect() {
    console.log('🔌 主动断开 WebSocket 连接');
    this.stopHeartbeat();
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.isConnected = false;
    this.messageHandlers.clear();
  }

  /**
   * 获取连接状态
   */
  getConnectionState() {
    if (!this.ws) return 'CLOSED';
    
    switch (this.ws.readyState) {
      case WebSocket.CONNECTING:
        return 'CONNECTING';
      case WebSocket.OPEN:
        return 'OPEN';
      case WebSocket.CLOSING:
        return 'CLOSING';
      case WebSocket.CLOSED:
        return 'CLOSED';
      default:
        return 'UNKNOWN';
    }
  }
}

// 创建单例
const wsService = new WebSocketService();

// 注册默认的消息处理器
setupDefaultHandlers(wsService);

export default wsService;

/**
 * 设置默认的消息处理器
 * @param {WebSocketService} ws - WebSocket 服务实例
 */
function setupDefaultHandlers(ws) {
  // 连接成功
  ws.on('connected', () => {
    console.log('✅ WebSocket 已连接');
  });

  // 心跳响应
  ws.on('pong', () => {
    // 心跳正常，无需处理
  });

  // 离线消息数通知
  ws.on('offline_message_count', (message) => {
    if (message.count > 0) {
      ElNotification({
        title: '离线消息',
        message: `您有 ${message.count} 条未读消息`,
        type: 'info',
        duration: 5000
      });
    }
  });

  // 私聊消息
  ws.on('private_message', (message) => {
    // 这里只处理通用逻辑，具体业务逻辑由各组件自行监听
    console.log('📩 收到私聊消息:', message);
  });

  // 群聊消息
  ws.on('group_message', (message) => {
    console.log('📩 收到群聊消息:', message);
  });

  // 笔记权限通知
  ws.on('note_permission', (message) => {
    ElNotification({
      title: '笔记共享通知',
      message: `用户 ${message.ownerName || '某用户'} 与你分享了笔记《${message.noteTitle || '未知笔记'}》`,
      type: 'success',
      duration: 5000
    });
  });

  // 编辑锁授予
  ws.on('edit_lock_granted', (message) => {
    console.log('🔓 获得编辑权限:', message.noteId);
  });

  // 编辑锁拒绝
  ws.on('edit_lock_denied', (message) => {
    ElMessage.warning(`该笔记正在被用户 ${message.editorId || '其他用户'} 编辑`);
  });

  // 编辑锁释放
  ws.on('edit_lock_released', (message) => {
    console.log('🔒 编辑权限已释放:', message.noteId);
  });

  // 笔记内容更新
  ws.on('note_content_updated', (message) => {
    console.log('📝 笔记内容已更新:', message.noteId);
  });
}
