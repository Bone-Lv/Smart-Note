import { reactive, onUnmounted } from 'vue';

// 全局WebSocket实例
let wsInstance = null;
let reconnectTimer = null;

export const useWebSocket = (onMessageCallback) => {
  const state = reactive({
    isConnected: false,
    messageQueue: [],
    reconnectAttempts: 0,
    maxReconnectAttempts: 5
  });

  const connect = (token) => {
    if (wsInstance && wsInstance.readyState === WebSocket.OPEN) {
      return;
    }

    const wsUrl = `ws://localhost:8080/ws?token=${token}`;
    
    try {
      wsInstance = new WebSocket(wsUrl);
      
      wsInstance.onopen = () => {
        console.log('WebSocket连接已建立');
        state.isConnected = true;
        state.reconnectAttempts = 0;
        
        // 发送连接成功消息
        send({ type: 'CONNECTED', data: {} });
        
        // 发送队列中的消息
        while (state.messageQueue.length > 0) {
          const message = state.messageQueue.shift();
          send(message);
        }
      };

      wsInstance.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          if (onMessageCallback) {
            onMessageCallback(message);
          }
        } catch (error) {
          console.error('解析WebSocket消息失败:', error);
        }
      };

      wsInstance.onclose = (event) => {
        console.log('WebSocket连接已关闭:', event.code, event.reason);
        state.isConnected = false;
        
        // 尝试重连
        if (state.reconnectAttempts < state.maxReconnectAttempts) {
          state.reconnectAttempts++;
          console.log(`尝试重连 (${state.reconnectAttempts}/${state.maxReconnectAttempts})...`);
          reconnectTimer = setTimeout(() => connect(token), 3000);
        }
      };

      wsInstance.onerror = (error) => {
        console.error('WebSocket错误:', error);
      };
    } catch (error) {
      console.error('创建WebSocket连接失败:', error);
    }
  };

  const disconnect = () => {
    if (wsInstance) {
      wsInstance.close();
      wsInstance = null;
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  const send = (message) => {
    if (!wsInstance || wsInstance.readyState !== WebSocket.OPEN) {
      // 如果未连接，暂时放入队列
      state.messageQueue.push(message);
      return false;
    }

    try {
      wsInstance.send(JSON.stringify(message));
      return true;
    } catch (error) {
      console.error('发送WebSocket消息失败:', error);
      return false;
    }
  };

  const heartbeat = () => {
    if (state.isConnected) {
      send({ type: 'PING', data: {} });
    }
  };

  // 心跳定时器
  const heartbeatInterval = setInterval(heartbeat, 30000);

  // 在组件卸载时断开连接
  onUnmounted(() => {
    clearInterval(heartbeatInterval);
    disconnect();
  });

  return {
    state,
    connect,
    disconnect,
    send
  };
};