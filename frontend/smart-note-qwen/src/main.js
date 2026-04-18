import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import '@fortawesome/fontawesome-free/css/all.min.css';
import App from './App.vue';
import router from './router/index.js';
import { vLazy } from './directives/lazyLoad.js';
import { initPerformanceMonitoring } from './utils/performance.js';
import { useLoadingStore } from './stores/loadingStore.js';
import './assets/style.css';

const app = createApp(App);

// 注册 Pinia
const pinia = createPinia();
app.use(pinia);

// 注册路由
app.use(router);

// 注册 Element Plus
app.use(ElementPlus);

// 注册自定义指令
app.directive('lazy', vLazy);

// 初始化性能监控（仅生产环境）
initPerformanceMonitoring();

// 等待路由就绪后再重置 Loading 状态
router.isReady().then(() => {
  const loadingStore = useLoadingStore();
  loadingStore.resetLoading();
  console.log('✅ Loading 状态已重置为 0');
});

app.mount('#app');

console.log('🚀 Smart Note 应用已启动');
