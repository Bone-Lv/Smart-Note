/**
 * 性能监控工具
 * 用于测量和上报应用性能指标
 */

// 性能指标存储
const metrics = new Map();

/**
 * 开始性能测量
 * @param {string} name - 测量名称
 */
export const startPerf = (name) => {
  if (import.meta.env.PROD) {
    performance.mark(`${name}-start`);
  }
  metrics.set(name, { start: Date.now() });
};

/**
 * 结束性能测量
 * @param {string} name - 测量名称
 * @param {object} extraData - 额外数据
 */
export const endPerf = (name, extraData = {}) => {
  const metric = metrics.get(name);
  if (!metric) return;

  const duration = Date.now() - metric.start;
  metric.duration = duration;
  metric.extraData = extraData;

  if (import.meta.env.PROD) {
    performance.mark(`${name}-end`);
    performance.measure(name, `${name}-start`, `${name}-end`);
    
    // 上报性能数据
    reportMetric(name, duration, extraData);
  } else {
    // 开发环境输出到控制台
    console.log(`⏱️ [${name}] ${duration}ms`, extraData);
  }

  metrics.delete(name);
};

/**
 * 记录自定义指标
 * @param {string} name - 指标名称
 * @param {number} value - 指标值
 * @param {object} extraData - 额外数据
 */
export const recordMetric = (name, value, extraData = {}) => {
  if (import.meta.env.PROD) {
    reportMetric(name, value, extraData);
  } else {
    console.log(`📊 [${name}] ${value}`, extraData);
  }
};

/**
 * 上报性能指标到服务器
 * @param {string} name - 指标名称
 * @param {number} value - 指标值
 * @param {object} extraData - 额外数据
 */
const reportMetric = async (name, value, extraData = {}) => {
  try {
    // 这里可以集成到你的监控系统
    // 例如：Sentry, LogRocket, 或自定义的后端接口
    
    const payload = {
      name,
      value,
      timestamp: Date.now(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      ...extraData
    };

    // 示例：发送到后端
    // await fetch('/api/performance', {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify(payload)
    // });

    // 开发环境打印
    if (!import.meta.env.PROD) {
      console.log('📤 性能数据:', payload);
    }
  } catch (error) {
    console.error('❌ 上报性能数据失败:', error);
  }
};

/**
 * 获取 Web Vitals 指标
 */
export const getWebVitals = () => {
  if (!import.meta.env.PROD) return;

  // FCP (First Contentful Paint)
  const fcpEntry = performance.getEntriesByName('first-contentful-paint')[0];
  if (fcpEntry) {
    recordMetric('FCP', fcpEntry.startTime, { type: 'paint' });
  }

  // LCP (Largest Contentful Paint)
  new PerformanceObserver((list) => {
    const entries = list.getEntries();
    const lastEntry = entries[entries.length - 1];
    recordMetric('LCP', lastEntry.startTime, { type: 'paint' });
  }).observe({ entryTypes: ['largest-contentful-paint'] });

  // FID (First Input Delay)
  new PerformanceObserver((list) => {
    const entries = list.getEntries();
    entries.forEach((entry) => {
      recordMetric('FID', entry.processingStart - entry.startTime, { type: 'interaction' });
    });
  }).observe({ entryTypes: ['first-input'] });

  // CLS (Cumulative Layout Shift)
  let clsValue = 0;
  new PerformanceObserver((list) => {
    list.getEntries().forEach((entry) => {
      if (!entry.hadRecentInput) {
        clsValue += entry.value;
        recordMetric('CLS', clsValue, { type: 'layout' });
      }
    });
  }).observe({ entryTypes: ['layout-shift'] });
};

/**
 * 监控页面加载性能
 */
export const monitorPageLoad = () => {
  if (!import.meta.env.PROD) return;

  window.addEventListener('load', () => {
    setTimeout(() => {
      const navigation = performance.getEntriesByType('navigation')[0];
      if (navigation) {
        recordMetric('Page Load Time', navigation.loadEventEnd - navigation.fetchStart, {
          dns: navigation.domainLookupEnd - navigation.domainLookupStart,
          tcp: navigation.connectEnd - navigation.connectStart,
          ttfb: navigation.responseStart - navigation.requestStart,
          download: navigation.responseEnd - navigation.responseStart,
          domParse: navigation.domInteractive - navigation.responseEnd,
          domReady: navigation.domContentLoadedEventEnd - navigation.fetchStart
        });
      }
    }, 0);
  });
};

/**
 * 监控 API 请求性能
 * @param {string} apiUrl - API URL
 * @param {Function} apiCall - API 调用函数
 */
export const monitorApiCall = async (apiUrl, apiCall) => {
  const startTime = Date.now();
  
  try {
    const result = await apiCall();
    const duration = Date.now() - startTime;
    
    recordMetric('API Request', duration, {
      url: apiUrl,
      status: 'success',
      method: 'GET' // 可以根据实际情况调整
    });
    
    return result;
  } catch (error) {
    const duration = Date.now() - startTime;
    
    recordMetric('API Request', duration, {
      url: apiUrl,
      status: 'error',
      error: error.message
    });
    
    throw error;
  }
};

/**
 * 初始化性能监控
 */
export const initPerformanceMonitoring = () => {
  if (!import.meta.env.PROD) {
    console.log('ℹ️ 开发模式，性能监控已跳过');
    return;
  }

  console.log('🚀 初始化性能监控...');
  
  // 获取 Web Vitals
  getWebVitals();
  
  // 监控页面加载
  monitorPageLoad();
  
  // 监控资源加载
  window.addEventListener('load', () => {
    setTimeout(() => {
      const resources = performance.getEntriesByType('resource');
      const slowResources = resources.filter(r => r.duration > 1000);
      
      if (slowResources.length > 0) {
        recordMetric('Slow Resources', slowResources.length, {
          resources: slowResources.map(r => ({
            name: r.name,
            duration: r.duration,
            type: r.initiatorType
          }))
        });
      }
    }, 5000);
  });

  console.log('✅ 性能监控初始化完成');
};