/**
 * SSE 流式输出处理工具
 */

/**
 * 处理 SSE 流式响应
 * @param {Response} response - Fetch API 响应对象
 * @param {function} onChunk - 接收到数据块的回调函数
 * @param {function} onComplete - 流结束的回调函数
 * @param {function} onError - 错误的回调函数
 * @param {AbortSignal} signal - 中止信号
 */
export const processSSEStream = async (
  response,
  onChunk,
  onComplete,
  onError,
  signal = null
) => {
  try {
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();

      if (done) {
        onComplete?.();
        break;
      }

      // 检查是否被中止
      if (signal?.aborted) {
        reader.cancel();
        break;
      }

      // 解码数据
      buffer += decoder.decode(value, { stream: true });

      // 解析 SSE 数据
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 保留不完整的行

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim();
          if (data) {
            onChunk?.(data);
          }
        }
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      // 用户主动中止，不视为错误
      onComplete?.();
    } else {
      onError?.(error);
    }
  }
};

/**
 * 创建中止控制器
 * @returns {AbortController}
 */
export const createAbortController = () => {
  return new AbortController();
};

/**
 * 中止流式输出
 * @param {AbortController} controller - 中止控制器
 */
export const abortStream = (controller) => {
  if (controller) {
    controller.abort();
  }
};
