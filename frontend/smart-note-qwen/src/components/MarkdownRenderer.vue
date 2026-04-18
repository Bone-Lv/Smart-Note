<template>
  <div class="markdown-renderer">
    <div 
      ref="contentRef" 
      class="markdown-content"
      v-html="renderedContent"
    ></div>
    
    <!-- 批注高亮区域 -->
    <div 
      v-for="(annotation, index) in annotationsWithPositions" 
      :key="annotation.id"
      class="annotation-overlay"
      :style="getAnnotationStyle(annotation)"
      @click="showAnnotationDetails(annotation)"
    >
      <span class="annotation-indicator">📝</span>
    </div>
    
    <!-- 批注详情弹窗 -->
    <div v-if="showAnnotationPopup" class="annotation-popup" :style="popupPosition">
      <div class="annotation-header">
        <h4>批注详情</h4>
        <button @click="showAnnotationPopup = false" class="close-btn">×</button>
      </div>
      <div class="annotation-body">
        <p><strong>作者:</strong> {{ selectedAnnotation.username }}</p>
        <p><strong>内容:</strong> {{ selectedAnnotation.content }}</p>
        <p><strong>目标内容:</strong> {{ selectedAnnotation.targetContent }}</p>
        <small>创建时间: {{ formatDate(selectedAnnotation.createTime) }}</small>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, watchEffect } from 'vue';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import katex from 'katex';
import 'highlight.js/styles/github.css';
import 'katex/dist/katex.min.css';

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
    }
  },
  emits: ['annotation-click', 'headings-change'],
  setup(props, { emit }) {
    const contentRef = ref(null);
    const renderedContent = ref('');
    const showAnnotationPopup = ref(false);
    const selectedAnnotation = ref(null);
    const popupPosition = ref({ top: '0px', left: '0px' });
    
    const md = new MarkdownIt({
      html: true,
      linkify: true,
      typographer: true,
      highlight: function (str, lang) {
        if (lang && hljs.getLanguage(lang)) {
          try {
            return '<pre class="hljs"><code>' +
                    hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
                    '</code></pre>';
          } catch (__) {}
        }

        return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
      }
    });

    // 自定义LaTeX公式渲染规则
    md.inline.ruler.after('escape', 'katex_inline', function(state, silent) {
      const delimiters = [['$', '$'], ['\\(', '\\)']];
      
      for (const [open, close] of delimiters) {
        const start = state.pos;
        
        if (state.src.slice(start, start + open.length) !== open) {
          continue;
        }
        
        const end = state.src.indexOf(close, start + open.length);
        if (end === -1) {
          continue;
        }
        
        const tex = state.src.slice(start + open.length, end);
        
        if (!silent) {
          try {
            const rendered = katex.renderToString(tex, {
              throwOnError: false,
              displayMode: false
            });
            const token = state.push('html_inline', '', 0);
            token.content = rendered;
          } catch (e) {
            const token = state.push('text', '', 0);
            token.content = open + tex + close;
          }
        }
        
        state.pos = end + close.length;
        return true;
      }
      
      return false;
    });

    // 自定义块级LaTeX公式渲染规则
    md.block.ruler.after('blockquote', 'katex_block', function(state, startLine, endLine, silent) {
      const lines = [];
      let line = startLine;
      const delimiter = '$$';
      
      // 检查是否以$$开头
      const firstLine = state.src.slice(state.bMarks[line] + state.tShift[line], state.eMarks[line]);
      if (!firstLine.trim().startsWith(delimiter)) {
        return false;
      }
      
      // 收集所有行直到找到结束的$$
      while (line < endLine) {
        const currentLine = state.src.slice(state.bMarks[line] + state.tShift[line], state.eMarks[line]);
        lines.push(currentLine);
        
        if (currentLine.trim().endsWith(delimiter) && lines.length > 1) {
          break;
        }
        line++;
      }
      
      if (line >= endLine) {
        return false;
      }
      
      if (!silent) {
        const tex = lines.join('\n').replace(/^\$\$|\$\$$/g, '').trim();
        try {
          const rendered = katex.renderToString(tex, {
            throwOnError: false,
            displayMode: true
          });
          const token = state.push('html_block', '', 0);
          token.content = `<div class="katex-block">${rendered}</div>`;
          token.map = [startLine, line + 1];
        } catch (e) {
          const token = state.push('paragraph_open', 'p', 1);
          token.map = [startLine, line + 1];
          const textToken = state.push('inline', '', 0);
          textToken.content = lines.join('\n');
          textToken.map = [startLine, line + 1];
          state.push('paragraph_close', 'p', -1);
        }
      }
      
      state.line = line + 1;
      return true;
    });

    // 增强标题渲染，添加锚点和ID
    const defaultHeadingRender = md.renderer.rules.heading_open || function(tokens, idx, options, env, self) {
      return self.renderToken(tokens, idx, options);
    };

    md.renderer.rules.heading_open = function(tokens, idx, options, env, self) {
      const token = tokens[idx];
      const nextToken = tokens[idx + 1];
      
      if (nextToken && nextToken.type === 'inline') {
        const headingText = nextToken.content;
        const headingId = 'heading-' + headingText
          .toLowerCase()
          .replace(/[^\w\u4e00-\u9fff]+/g, '-')
          .replace(/(^-|-$)/g, '');
        
        token.attrSet('id', headingId);
        token.attrSet('class', 'markdown-heading');
      }
      
      return defaultHeadingRender(tokens, idx, options, env, self);
    };

    const annotationsWithPositions = ref([]);

    const renderMarkdown = () => {
      renderedContent.value = md.render(props.content || '');
      
      nextTick(() => {
        calculateAnnotationPositions();
        extractHeadings();
      });
    };

    // 提取标题用于导航
    const extractHeadings = () => {
      if (!contentRef.value) return;
      
      const headings = [];
      const headingElements = contentRef.value.querySelectorAll('h1, h2, h3, h4, h5, h6');
      
      headingElements.forEach((heading, index) => {
        const level = parseInt(heading.tagName.substring(1));
        const text = heading.textContent.trim();
        const id = heading.id || `heading-${index}`;
        
        headings.push({
          level,
          text,
          id,
          element: heading
        });
      });
      
      emit('headings-change', headings);
    };

    const calculateAnnotationPositions = () => {
      if (!contentRef.value) return;
      
      const textContent = contentRef.value.textContent || '';
      annotationsWithPositions.value = props.annotations.map(annotation => {
        const start = Math.max(0, annotation.startPosition);
        const end = Math.min(textContent.length, annotation.endPosition);
        
        // 在DOM中找到对应位置的元素
        const range = document.createRange();
        range.selectNodeContents(contentRef.value);
        
        try {
          const startContainer = findTextAtPosition(contentRef.value, start);
          const endContainer = findTextAtPosition(contentRef.value, end);
          
          if (startContainer && endContainer) {
            const rect = startContainer.getBoundingClientRect();
            const parentRect = contentRef.value.getBoundingClientRect();
            
            return {
              ...annotation,
              top: rect.top - parentRect.top + window.scrollY,
              left: rect.left - parentRect.left,
              width: rect.width,
              height: rect.height
            };
          }
        } catch (e) {
          console.warn('Could not calculate annotation position:', e);
        }
        
        return annotation;
      });
    };

    const findTextAtPosition = (element, position) => {
      let currentPos = 0;
      const walker = document.createTreeWalker(
        element,
        NodeFilter.SHOW_TEXT,
        null,
        false
      );

      let node;
      while (node = walker.nextNode()) {
        const nodeLength = node.nodeValue.length;
        if (position >= currentPos && position < currentPos + nodeLength) {
          return node;
        }
        currentPos += nodeLength;
      }
      return null;
    };

    const getAnnotationStyle = (annotation) => {
      return {
        position: 'absolute',
        top: `${annotation.top}px`,
        left: `${annotation.left}px`,
        width: `${annotation.width}px`,
        height: `${annotation.height}px`,
        backgroundColor: 'rgba(255, 223, 0, 0.3)',
        border: '1px dashed #ffd700',
        borderRadius: '3px',
        cursor: 'pointer',
        zIndex: 10
      };
    };

    const showAnnotationDetails = (annotation) => {
      selectedAnnotation.value = annotation;
      showAnnotationPopup.value = true;
      
      // 计算弹窗位置
      const rect = contentRef.value.getBoundingClientRect();
      popupPosition.value = {
        top: `${annotation.top + annotation.height + 10}px`,
        left: `${Math.max(0, annotation.left)}px`
      };
      
      emit('annotation-click', annotation);
    };

    const formatDate = (dateString) => {
      return new Date(dateString).toLocaleString();
    };

    // 监听props变化重新渲染
    watchEffect(() => {
      renderMarkdown();
    });

    return {
      contentRef,
      renderedContent,
      annotationsWithPositions,
      showAnnotationPopup,
      selectedAnnotation,
      popupPosition,
      getAnnotationStyle,
      showAnnotationDetails,
      formatDate
    };
  }
};
</script>

<style scoped>
.markdown-renderer {
  position: relative;
  min-height: 400px;
}

.markdown-content {
  line-height: 1.6;
  color: #333;
}

/* 标题样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
  cursor: pointer;
  transition: color 0.2s;
}

.markdown-content :deep(h1:hover),
.markdown-content :deep(h2:hover),
.markdown-content :deep(h3:hover),
.markdown-content :deep(h4:hover),
.markdown-content :deep(h5:hover),
.markdown-content :deep(h6:hover) {
  color: #409eff;
}

.markdown-content :deep(h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.25em;
}

.markdown-content :deep(h4) {
  font-size: 1em;
}

.markdown-content :deep(h5) {
  font-size: 0.875em;
}

.markdown-content :deep(h6) {
  font-size: 0.85em;
  color: #6a737d;
}

/* 代码块样式 */
.markdown-content :deep(pre) {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 6px;
  margin: 16px 0;
}

.markdown-content :deep(code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-content :deep(pre code) {
  padding: 0;
  margin: 0;
  font-size: 100%;
  background-color: transparent;
  border: 0;
}

/* 列表样式 */
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 2em;
  margin: 16px 0;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

/* 引用样式 */
.markdown-content :deep(blockquote) {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 16px 0;
}

.markdown-content :deep(blockquote > :first-child) {
  margin-top: 0;
}

.markdown-content :deep(blockquote > :last-child) {
  margin-bottom: 0;
}

/* 链接样式 */
.markdown-content :deep(a) {
  color: #0366d6;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

/* 图片样式 */
.markdown-content :deep(img) {
  max-width: 100%;
  box-sizing: content-box;
  background-color: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
  margin: 16px 0;
}

/* 表格样式 */
.markdown-content :deep(table) {
  border-spacing: 0;
  border-collapse: collapse;
  margin: 16px 0;
  width: 100%;
  overflow: auto;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  padding: 6px 13px;
  border: 1px solid #dfe2e5;
}

.markdown-content :deep(table tr) {
  background-color: #fff;
  border-top: 1px solid #c6cbd1;
}

.markdown-content :deep(table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

.markdown-content :deep(table th) {
  font-weight: 600;
}

/* LaTeX公式样式 */
.markdown-content :deep(.katex) {
  font-size: 1.1em;
}

.markdown-content :deep(.katex-block) {
  margin: 16px 0;
  padding: 16px;
  overflow-x: auto;
  text-align: center;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.markdown-content :deep(.katex-display) {
  margin: 0;
}

/* 水平线样式 */
.markdown-content :deep(hr) {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #e1e4e8;
  border: 0;
}

/* 批注样式 */
.annotation-overlay {
  position: absolute;
  pointer-events: auto;
  transition: all 0.2s ease;
}

.annotation-overlay:hover {
  opacity: 0.8;
  transform: scale(1.02);
}

.annotation-indicator {
  position: absolute;
  top: -12px;
  right: -12px;
  background: #ff6b6b;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  z-index: 20;
}

.annotation-popup {
  position: fixed;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1000;
  width: 300px;
  max-width: 90vw;
  font-size: 14px;
  backdrop-filter: blur(10px);
}

.annotation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
  border-radius: 8px 8px 0 0;
}

.annotation-header h4 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.annotation-body {
  padding: 16px;
}

.annotation-body p {
  margin: 8px 0;
}

.annotation-body small {
  color: #666;
}

.annotation-body strong {
  color: #333;
}
</style>
