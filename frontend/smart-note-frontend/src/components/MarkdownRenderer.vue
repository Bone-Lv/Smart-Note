<template>
  <div class="markdown-renderer">
    <div ref="contentRef" class="markdown-content" v-html="renderedContent"></div>
    
    <!-- 批注区域 -->
    <div v-if="showAnnotations" class="annotations-container">
      <div 
        v-for="annotation in annotations" 
        :key="annotation.id"
        class="annotation-item"
        :style="{ top: `${calculateAnnotationPosition(annotation)}px` }"
      >
        <div class="annotation-content">
          <div class="annotation-text">{{ annotation.content }}</div>
          <div class="annotation-meta">
            <span class="annotation-author">{{ annotation.username }}</span>
            <span class="annotation-time">{{ formatDate(annotation.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import marked from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/default.css';

export default {
  name: 'MarkdownRenderer',
  props: {
    content: {
      type: String,
      default: ''
    },
    annotations: {
      type: Array,
      default: () => []
    },
    showAnnotations: {
      type: Boolean,
      default: true
    },
    version: {
      type: Number,
      default: 1
    }
  },
  data() {
    return {
      renderedContent: '',
      contentRef: null
    };
  },
  watch: {
    content: {
      immediate: true,
      handler(newContent) {
        this.renderMarkdown(newContent);
      }
    },
    version() {
      // 版本变化时重新渲染
      this.renderMarkdown(this.content);
    }
  },
  mounted() {
    this.contentRef = this.$refs.contentRef;
    this.renderMarkdown(this.content);
  },
  methods: {
    renderMarkdown(content) {
      marked.setOptions({
        highlight: function(code, lang) {
          const language = hljs.getLanguage(lang) ? lang : 'plaintext';
          return hljs.highlight(code, { language }).value;
        },
        breaks: true,
        gfm: true
      });

      this.renderedContent = marked.parse(content || '');
    },

    calculateAnnotationPosition(annotation) {
      // 根据批注的起始位置计算在页面上的垂直位置
      if (!this.contentRef) return 0;
      
      // 简化的计算方式：根据字符位置估算行数
      const contentBefore = this.content.substring(0, annotation.startPosition);
      const lines = contentBefore.split('\n').length;
      return lines * 20; // 每行约20px
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString();
    }
  }
};
</script>

<style scoped>
.markdown-renderer {
  position: relative;
}

.markdown-content {
  line-height: 1.6;
  font-size: 16px;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin-top: 24px;
  margin-bottom: 16px;
}

.markdown-content p {
  margin-bottom: 16px;
}

.markdown-content pre {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}

.markdown-content code {
  background-color: #f6f8fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: monospace;
}

.annotations-container {
  position: absolute;
  right: 0;
  top: 0;
  width: 300px;
  max-height: 100%;
  overflow-y: auto;
}

.annotation-item {
  position: absolute;
  background: #fff9db;
  border-left: 3px solid #ffd43b;
  padding: 8px 12px;
  margin: 4px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 10;
  min-width: 250px;
}

.annotation-content {
  font-size: 14px;
}

.annotation-text {
  color: #24292f;
  margin-bottom: 4px;
}

.annotation-meta {
  font-size: 12px;
  color: #656d76;
}

.annotation-author {
  font-weight: bold;
  margin-right: 8px;
}
</style>