<template>
  <div 
    ref="containerRef"
    class="virtual-list-container"
    :style="containerStyle"
    @scroll="handleScroll"
  >
    <div 
      class="virtual-list-phantom"
      :style="{ height: totalHeight + 'px' }"
    >
      <div 
        class="virtual-list-content"
        :style="{ transform: `translateY(${offsetY}px)` }"
      >
        <slot 
          v-for="item in visibleData" 
          :key="item.key || item.id"
          :item="item.data"
          :index="item.index"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  // 数据列表
  list: {
    type: Array,
    required: true,
    default: () => []
  },
  // 每项高度
  itemHeight: {
    type: Number,
    required: true
  },
  // 容器高度
  height: {
    type: Number,
    required: true
  },
  // 预加载数量（上下各预加载多少项）
  buffer: {
    type: Number,
    default: 5
  }
});

const containerRef = ref(null);
const scrollTop = ref(0);

// 可见区域的项目数量
const visibleCount = computed(() => {
  return Math.ceil(props.height / props.itemHeight);
});

// 总高度
const totalHeight = computed(() => {
  return props.list.length * props.itemHeight;
});

// 开始索引
const startIndex = computed(() => {
  const index = Math.floor(scrollTop.value / props.itemHeight);
  return Math.max(0, index - props.buffer);
});

// 结束索引
const endIndex = computed(() => {
  const index = startIndex.value + visibleCount.value + props.buffer * 2;
  return Math.min(props.list.length, index);
});

// 偏移量
const offsetY = computed(() => {
  return startIndex.value * props.itemHeight;
});

// 可见数据
const visibleData = computed(() => {
  return props.list.slice(startIndex.value, endIndex.value).map((item, index) => ({
    data: item,
    index: startIndex.value + index,
    key: item.id || item.key || startIndex.value + index
  }));
});

// 容器样式
const containerStyle = computed(() => ({
  height: props.height + 'px',
  overflow: 'auto'
}));

// 处理滚动
const handleScroll = (event) => {
  scrollTop.value = event.target.scrollTop;
};

// 滚动到指定索引
const scrollToIndex = (index) => {
  if (containerRef.value) {
    containerRef.value.scrollTop = index * props.itemHeight;
  }
};

// 滚动到顶部
const scrollToTop = () => {
  scrollToIndex(0);
};

// 滚动到底部
const scrollToBottom = () => {
  scrollToIndex(props.list.length - 1);
};

onMounted(() => {
  console.log('✅ 虚拟列表初始化，总数:', props.list.length, '可见:', visibleCount.value);
});

onUnmounted(() => {
  console.log('🧹 虚拟列表卸载');
});

defineExpose({
  scrollToIndex,
  scrollToTop,
  scrollToBottom
});
</script>

<style scoped>
.virtual-list-container {
  position: relative;
  width: 100%;
}

.virtual-list-phantom {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  z-index: -1;
}

.virtual-list-content {
  left: 0;
  right: 0;
  top: 0;
  position: absolute;
}
</style>