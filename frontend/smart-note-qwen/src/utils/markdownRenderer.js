import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import katex from 'katex';
import 'highlight.js/styles/atom-one-dark.css';
import 'katex/dist/katex.min.css';

// 创建Markdown实例并配置插件
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    // 代码高亮
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>';
      } catch (__) {}
    }
    
    // 默认高亮
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
  }
});

// LaTeX公式渲染插件
md.inline.ruler.before('escape', 'katex_inline', function(state, silent) {
  const start = state.pos;
  const max = state.posMax;
  
  // 检查是否是行内公式 $...$
  if (state.src.charCodeAt(start) !== 0x24/* $ */) return false;
  if (start + 1 >= max) return false;
  
  let pos = start + 1;
  while (pos < max) {
    if (state.src.charCodeAt(pos) === 0x24/* $ */) {
      // 找到结束的 $
      const content = state.src.slice(start + 1, pos);
      
      if (!silent) {
        try {
          const rendered = katex.renderToString(content, {
            throwOnError: false,
            displayMode: false
          });
          
          const token = state.push('html_inline', '', 0);
          token.content = rendered;
        } catch (e) {
          const token = state.push('text', '', 0);
          token.content = state.src.slice(start, pos + 1);
        }
      }
      
      state.pos = pos + 1;
      return true;
    }
    pos++;
  }
  
  return false;
});

// 块级公式渲染插件 $$...$$
md.block.ruler.before('paragraph', 'katex_block', function(state, startLine, endLine, silent) {
  const start = state.bMarks[startLine] + state.tShift[startLine];
  const max = state.eMarks[startLine];
  
  // 检查是否是块级公式 $$...$$
  if (state.src.charCodeAt(start) !== 0x24/* $ */) return false;
  if (state.src.charCodeAt(start + 1) !== 0x24/* $ */) return false;
  
  // 查找结束的 $$
  let line = startLine;
  let found = false;
  
  while (line < endLine) {
    const lineStart = state.bMarks[line] + state.tShift[line];
    const lineMax = state.eMarks[line];
    const lineContent = state.src.slice(lineStart, lineMax);
    
    if (lineContent.trim().endsWith('$$')) {
      found = true;
      break;
    }
    line++;
  }
  
  if (!found) return false;
  
  if (!silent) {
    const content = state.src.slice(start + 2, state.eMarks[line]).trim();
    
    try {
      const rendered = katex.renderToString(content, {
        throwOnError: false,
        displayMode: true
      });
      
      const token = state.push('html_block', '', 0);
      token.content = `<div class="katex-block">${rendered}</div>`;
      token.map = [startLine, line + 1];
    } catch (e) {
      const token = state.push('paragraph_open', 'p', 0);
      token.map = [startLine, line + 1];
      
      const textToken = state.push('text', '', 0);
      textToken.content = state.src.slice(start, state.eMarks[line]);
      
      state.push('paragraph_close', 'p', -1);
    }
  }
  
  state.line = line + 1;
  return true;
});

export default md;
