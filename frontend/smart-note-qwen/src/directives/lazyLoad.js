/**
 * 图片懒加载自定义指令
 * 使用 Intersection Observer API 实现
 */

// 存储所有懒加载图片的观察者
const imageObserver = new IntersectionObserver((entries, observer) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const img = entry.target;
      const src = img.dataset.src;
      
      if (src) {
        // 创建临时图片对象预加载
        const tempImg = new Image();
        
        tempImg.onload = () => {
          img.src = src;
          img.classList.add('loaded');
          img.classList.remove('lazy');
          console.log('✅ 图片加载成功:', src);
        };
        
        tempImg.onerror = () => {
          img.classList.add('error');
          img.classList.remove('lazy');
          console.error('❌ 图片加载失败:', src);
        };
        
        tempImg.src = src;
        
        // 停止观察
        observer.unobserve(img);
      }
    }
  });
}, {
  rootMargin: '50px 0px', // 提前50px开始加载
  threshold: 0.01
});

/**
 * 懒加载指令
 * 用法: v-lazy="imageUrl"
 */
export const vLazy = {
  mounted(el, binding) {
    // 设置初始状态
    el.classList.add('lazy');
    el.dataset.src = binding.value;
    
    // 可以设置占位图
    el.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"%3E%3C/svg%3E';
    
    // 开始观察
    imageObserver.observe(el);
  },
  
  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      el.dataset.src = binding.value;
      
      // 如果已经在视口中，立即加载
      const rect = el.getBoundingClientRect();
      if (rect.top < window.innerHeight && rect.bottom > 0) {
        el.src = binding.value;
        imageObserver.unobserve(el);
      }
    }
  },
  
  unmounted(el) {
    // 停止观察
    imageObserver.unobserve(el);
  }
};

/**
 * 批量懒加载图片
 * @param {HTMLElement[]} images - 图片元素数组
 */
export const lazyLoadImages = (images) => {
  images.forEach(img => {
    if (img.dataset.src) {
      imageObserver.observe(img);
    }
  });
};

/**
 * 清除所有观察
 */
export const clearAllObservers = () => {
  imageObserver.disconnect();
};