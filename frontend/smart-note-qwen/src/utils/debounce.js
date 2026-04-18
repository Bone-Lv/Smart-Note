/**
 * 防抖函数
 * @param {Function} fn - 需要防抖的函数
 * @param {number} delay - 延迟时间（毫秒），默认300ms
 * @returns {Function} 防抖后的函数
 */
export const debounce = (fn, delay = 300) => {
  let timer = null;
  
  return function(...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};

/**
 * 节流函数
 * @param {Function} fn - 需要节流的函数
 * @param {number} delay - 延迟时间（毫秒），默认300ms
 * @returns {Function} 节流后的函数
 */
export const throttle = (fn, delay = 300) => {
  let lastTime = 0;
  
  return function(...args) {
    const now = Date.now();
    if (now - lastTime >= delay) {
      lastTime = now;
      fn.apply(this, args);
    }
  };
};

/**
 * 带立即执行选项的防抖函数
 * @param {Function} fn - 需要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @param {boolean} immediate - 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export const debounceWithImmediate = (fn, delay = 300, immediate = false) => {
  let timer = null;
  let result = null;
  
  return function(...args) {
    if (immediate && !timer) {
      // 立即执行
      result = fn.apply(this, args);
    }
    
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      if (!immediate) {
        result = fn.apply(this, args);
      }
      timer = null;
    }, delay);
    
    return result;
  };
};

/**
 * 带前缘和后缘的节流函数
 * @param {Function} fn - 需要节流的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @param {object} options - 配置项
 * @param {boolean} options.leading - 是否在开始时执行
 * @param {boolean} options.trailing - 是否在结束时执行
 * @returns {Function} 节流后的函数
 */
export const throttleAdvanced = (fn, delay = 300, options = {}) => {
  const { leading = true, trailing = true } = options;
  let lastTime = 0;
  let timer = null;
  
  return function(...args) {
    const now = Date.now();
    
    if (!lastTime && !leading) {
      lastTime = now;
    }
    
    const remaining = delay - (now - lastTime);
    
    if (remaining <= 0 || remaining > delay) {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      lastTime = now;
      fn.apply(this, args);
    } else if (!timer && trailing) {
      timer = setTimeout(() => {
        lastTime = leading ? Date.now() : 0;
        timer = null;
        fn.apply(this, args);
      }, remaining);
    }
  };
};
