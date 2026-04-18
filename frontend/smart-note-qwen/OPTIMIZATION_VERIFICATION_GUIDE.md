# 性能优化验证指南

本文档帮助你验证已实施的性能优化是否生效。

## 🔍 验证方法

### 1. 验证图片懒加载

**步骤**:
1. 打开聊天页面（私聊或群聊）
2. 打开浏览器开发者工具 → Network标签
3. 刷新页面
4. 观察图片加载情况

**预期结果**:
- ✅ 只有可视区域内的图片会立即加载
- ✅ 滚动后，新进入可视区域的图片才开始加载
- ✅ img标签有 `loading="lazy"` 属性

**检查代码**:
```html
<!-- 应该看到 -->
<img src="..." loading="lazy" decoding="async" alt="...">
```

**控制台验证**:
```javascript
// 在控制台运行
const lazyImages = document.querySelectorAll('img[loading="lazy"]');
console.log('Lazy loaded images:', lazyImages.length);
```

---

### 2. 验证Markdown渲染缓存

**步骤**:
1. 打开AI聊天窗口
2. 发送一条包含Markdown的消息
3. 向上滚动查看历史消息
4. 再次向下滚动

**预期结果**:
- ✅ 第二次查看相同消息时，渲染速度明显更快
- ✅ 控制台无重复渲染警告

**代码验证**:
```javascript
// 在浏览器控制台运行
import { useAiStore } from '@/stores/aiStore.js';
const aiStore = useAiStore();

// 查看组件实例
const component = document.querySelector('.virtual-message-list').__vueParentComponent;
console.log('Cache size:', component.setupContext.renderCache?.size || 'N/A');
```

**性能测试**:
```javascript
// 测试渲染时间
const content = '# Test\n\n```js\ncode\n```\n\n$E=mc^2$';

console.time('First render');
const result1 = md.render(content);
console.timeEnd('First render'); // ~50ms

console.time('Cached render');
const result2 = md.render(content);
console.timeEnd('Cached render'); // ~1ms (从缓存)
```

---

### 3. 验证按需加载语言包

**步骤**:
1. 打开应用
2. 开发者工具 → Network标签
3. 筛选 "JS" 文件
4. 发送包含代码块的消息

**预期结果**:
- ✅ 初始加载时，没有 `languages-xxx.js` 文件
- ✅ 首次遇到代码块时，动态加载对应语言包
- ✅ 同一语言第二次不重复加载

**检查Network**:
```
初始加载:
  ├── index.js
  └── vendor.js
  
发送JavaScript代码后:
  └── languages-javascript.js  ← 动态加载

发送Python代码后:
  └── languages-python.js  ← 动态加载
```

**控制台验证**:
```javascript
// 查看已加载的语言
import hljs from 'highlight.js/lib/core';
console.log('Available languages:', Object.keys(hljs.listLanguages()));
```

**包体积对比**:
```bash
# 优化前
npm run build
# dist/assets/index.js: 250KB

# 优化后
npm run build
# dist/assets/index.js: 100KB
# dist/assets/languages-*.js: 各5-10KB（按需加载）
```

---

### 4. 验证Web Worker（如果启用）

**步骤**:
1. 在组件中启用Worker: `useWorkerRender.value = true`
2. 发送包含复杂Markdown的消息
3. 开发者工具 → Performance标签
4. 录制性能

**预期结果**:
- ✅ Main线程无长时间任务（>50ms）
- ✅ Worker线程处理Markdown渲染
- ✅ UI保持响应（可滚动、可点击）

**检查Worker**:
```javascript
// 在控制台
console.log('Workers:', performance.getEntriesByType('worker'));
```

**性能对比**:
```javascript
// 主线程渲染
console.time('Main thread');
md.render(complexMarkdown);
console.timeEnd('Main thread'); // 阻塞UI

// Worker渲染
console.time('Worker');
await markdownWorkerManager.render(complexMarkdown);
console.timeEnd('Worker'); // 不阻塞UI
```

---

## 📊 性能基准测试

### 测试场景1: 大量消息滚动

**测试数据**:
```javascript
// 生成1000条测试消息
const messages = Array.from({ length: 1000 }, (_, i) => ({
  id: i,
  role: i % 2 === 0 ? 'user' : 'assistant',
  content: `Message ${i}\n\nSome **markdown** content`,
  createTime: new Date().toISOString()
}));
```

**性能指标**:
| 指标 | 优化前 | 优化后 | 目标 |
|------|--------|--------|------|
| 首屏渲染时间 | 500ms | <20ms | ✅ |
| 滚动FPS | 30fps | 60fps | ✅ |
| DOM节点数 | 1000+ | ~20 | ✅ |
| 内存占用 | 50MB | 2MB | ✅ |

---

### 测试场景2: 代码高亮

**测试数据**:
```javascript
const codeMessage = `
Here's a JavaScript example:

\`\`\`javascript
function fibonacci(n) {
  if (n <= 1) return n;
  return fibonacci(n - 1) + fibonacci(n - 2);
}

console.log(fibonacci(10));
\`\`\`
`;
```

**性能指标**:
| 指标 | 优化前 | 优化后 | 目标 |
|------|--------|--------|------|
| 初始包体积 | 200KB | 50KB | ✅ |
| 首次高亮时间 | 5ms | 5ms + 加载 | ✅ |
| 二次高亮时间 | 5ms | <1ms (缓存) | ✅ |

---

### 测试场景3: LaTeX公式

**测试数据**:
```javascript
const formulaMessage = `
麦克斯韦方程组：

$$
\\begin{aligned}
\\nabla \\cdot \\mathbf{E} &= \\frac{\\rho}{\\varepsilon_0} \\\\
\\nabla \\cdot \\mathbf{B} &= 0 \\\\
\\nabla \\times \\mathbf{E} &= -\\frac{\\partial \\mathbf{B}}{\\partial t} \\\\
\\nabla \\times \\mathbf{B} &= \\mu_0\\left(\\mathbf{J} + \\varepsilon_0 \\frac{\\partial \\mathbf{E}}{\\partial t}\\right)
\\end{aligned}
$$
`;
```

**性能指标**:
| 指标 | 优化前 | 优化后 | 目标 |
|------|--------|--------|------|
| 渲染时间 | ~10ms | ~10ms | ✅ |
| 缓存命中时间 | - | <1ms | ✅ |

---

## 🎯 Lighthouse评分

运行Lighthouse审计，预期得分：

| 类别 | 优化前 | 优化后 | 目标 |
|------|--------|--------|------|
| **Performance** | 70 | 90+ | ✅ |
| **Accessibility** | 85 | 90+ | ✅ |
| **Best Practices** | 80 | 90+ | ✅ |
| **SEO** | 75 | 85+ | ✅ |

**运行Lighthouse**:
1. 开发者工具 → Lighthouse标签
2. 选择 "Mobile" 或 "Desktop"
3. 点击 "Analyze page load"
4. 查看报告

---

## 🔬 高级调试技巧

### 1. 监控缓存命中率

```javascript
// 在组件中添加
let cacheStats = { hits: 0, misses: 0 };

const renderMarkdown = (content) => {
  if (renderCache.has(content)) {
    cacheStats.hits++;
  } else {
    cacheStats.misses++;
  }
  
  const hitRate = cacheStats.hits / (cacheStats.hits + cacheStats.misses);
  console.log(`Cache hit rate: ${(hitRate * 100).toFixed(1)}%`);
  
};
```

### 2. 监控图片加载

```javascript
// 使用 Performance Observer
const observer = new PerformanceObserver((list) => {
  list.getEntries().forEach((entry) => {
    if (entry.initiatorType === 'img') {
      console.log('Image loaded:', entry.name, `in ${entry.duration.toFixed(0)}ms`);
    }
  });
});

observer.observe({ entryTypes: ['resource'] });
```

### 3. 监控语言包加载

```javascript
// 在markdownRenderer.js中
const originalLoadLanguage = loadLanguage;
loadLanguage = async (lang) => {
  console.time(`Load ${lang}`);
  await originalLoadLanguage(lang);
  console.timeEnd(`Load ${lang}`);
};
```

### 4. 监控Worker通信

```javascript
// 在markdownWorker.js中
const originalPostMessage = worker.postMessage;
worker.postMessage = (data) => {
  console.log('Sending to worker:', data.content.substring(0, 50) + '...');
  console.time('Worker render');
  return originalPostMessage.call(worker, data);
};

worker.onmessage = (e) => {
  console.timeEnd('Worker render');
  originalOnmessage(e);
};
```

---

## ✅ 验证清单

完成以下检查，确保优化生效：

### 图片懒加载
- [ ] 聊天页面中的图片有 `loading="lazy"` 属性
- [ ] Network面板显示图片按需加载
- [ ] 滚动到新区域时，图片才开始加载
- [ ] 旧浏览器正常显示图片（优雅降级）

### Markdown缓存
- [ ] 重复查看相同消息时，渲染速度更快
- [ ] 缓存大小不超过100条
- [ ] 缓存正确清理（LRU策略）
- [ ] 内存占用合理（<1MB）

### 按需加载
- [ ] 初始加载无语言包文件
- [ ] 首次遇到代码时，动态加载语言包
- [ ] 同一语言不重复加载
- [ ] 包体积减少约150KB

### Web Worker（如启用）
- [ ] Worker正确创建
- [ ] Markdown渲染在Worker线程
- [ ] 主线程不被阻塞
- [ ] 失败时优雅降级到主线程

### 整体性能
- [ ] Lighthouse Performance得分 >90
- [ ] 1000条消息滚动流畅（60fps）
- [ ] 首屏加载时间 <1秒
- [ ] 内存占用 <50MB

---

## 🐛 常见问题

### Q1: 图片懒加载不生效？

**检查**:
```javascript
// 1. 确认浏览器支持
console.log('loading' in HTMLImageElement.prototype); // true表示支持

// 2. 检查属性
const img = document.querySelector('img');
console.log(img.loading); // 应该是 "lazy"

// 3. 检查CSS
console.log(getComputedStyle(img).display); // 不能是 none
```

**解决**:
- 更新浏览器到最新版本
- 或使用 `vue-lazyload` 插件作为polyfill

### Q2: 缓存一直未命中？

**原因**: 内容有细微差异

**检查**:
```javascript
// 比较内容
const content1 = 'Hello';
const content2 = 'Hello '; // 多了空格

console.log(content1 === content2); // false
console.log(renderCache.has(content1)); // true
console.log(renderCache.has(content2)); // false
```

**解决**:
- 标准化内容（trim、统一换行符）
- 或增加缓存大小

### Q3: 语言包加载很慢？

**检查**:
```javascript
// 查看加载时间
console.time('Load JS');
await import('highlight.js/lib/languages/javascript');
console.timeEnd('Load JS'); // 应该 <100ms
```

**可能原因**:
- 网络慢
- CDN问题
- 文件太大

**解决**:
- 使用CDN加速
- 预加载常用语言
- 压缩语言包

### Q4: Worker创建失败？

**检查**:
```javascript
console.log('Worker support:', typeof Worker !== 'undefined');
```

**可能原因**:
- 浏览器不支持
- CSP策略限制
- 文件路径错误

**解决**:
- 自动降级到主线程
- 检查CSP配置
- 确认文件路径正确

---

## 📈 持续监控

### 生产环境监控

在实际使用中监控性能：

```javascript
// 上报性能数据
window.addEventListener('load', () => {
  const perfData = performance.getEntriesByType('navigation')[0];
  
  fetch('/api/performance', {
    method: 'POST',
    body: JSON.stringify({
      domContentLoaded: perfData.domContentLoadedEventEnd - perfData.fetchStart,
      fullyLoaded: perfData.loadEventEnd - perfData.fetchStart,
      cacheHitRate: getCacheHitRate(),
      lazyImageCount: document.querySelectorAll('img[loading="lazy"]').length
    })
  });
});
```

### 用户反馈

收集用户反馈：
- 页面加载是否更快？
- 滚动是否更流畅？
- 是否有显示问题？

---

**验证完成！** ✅

如果所有检查都通过，说明优化已成功实施并生效。享受更快的应用体验吧！🚀
