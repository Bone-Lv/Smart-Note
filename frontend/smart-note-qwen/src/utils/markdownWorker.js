7/**
 * Markdown渲染Worker管理器
 * 用于在Web Worker中渲染Markdown，避免阻塞主线程
 */

class MarkdownWorkerManager {
  constructor() {
    this.worker = null;
    this.pendingRenders = new Map();
    this.requestId = 0;
    this.useWorker = typeof Worker !== 'undefined';
  }

  // 初始化Worker
  initWorker() {
    if (!this.useWorker || this.worker) return;

    try {
      // 创建Worker
      this.worker = new Worker(new URL('../workers/markdown.worker.js', import.meta.url));
      
      // 监听Worker消息
      this.worker.onmessage = (e) => {
        const { id, result, error } = e.data;
        
        if (this.pendingRenders.has(id)) {
          const { resolve, reject } = this.pendingRenders.get(id);
          this.pendingRenders.delete(id);
          
          if (error) {
            reject(new Error(error));
          } else {
            resolve(result);
          }
        }
      };

      this.worker.onerror = (error) => {
        console.error('Markdown Worker error:', error);
        // 发生错误时，拒绝所有待处理的请求
        this.pendingRenders.forEach(({ reject }) => {
          reject(error);
        });
        this.pendingRenders.clear();
      };
    } catch (error) {
      console.warn('Failed to create Markdown Worker, falling back to main thread:', error);
      this.useWorker = false;
    }
  }

  // 渲染Markdown
  async render(content) {
    if (!content) return '';

    // 如果Worker不可用，直接在主线程渲染
    if (!this.useWorker) {
      const md = await import('../utils/markdownRenderer.js');
      return md.default.render(content);
    }

    // 初始化Worker（如果还未初始化）
    if (!this.worker) {
      this.initWorker();
    }

    // 如果Worker创建失败，降级到主线程
    if (!this.worker) {
      const md = await import('../utils/markdownRenderer.js');
      return md.default.render(content);
    }

    // 生成请求ID
    const id = ++this.requestId;

    // 返回Promise
    return new Promise((resolve, reject) => {
      // 存储回调
      this.pendingRenders.set(id, { resolve, reject });
      
      // 发送渲染请求
      this.worker.postMessage({ content, id });
      
      // 设置超时（5秒）
      setTimeout(() => {
        if (this.pendingRenders.has(id)) {
          this.pendingRenders.delete(id);
          reject(new Error('Markdown rendering timeout'));
        }
      }, 5000);
    });
  }

  // 销毁Worker
  destroy() {
    if (this.worker) {
      this.worker.terminate();
      this.worker = null;
    }
    this.pendingRenders.clear();
  }
}

// 创建单例
const markdownWorkerManager = new MarkdownWorkerManager();

export default markdownWorkerManager;
