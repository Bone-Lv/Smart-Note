// src/utils/markdown-it-plugins.ts
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { diffChars } from 'diff'

// 为MarkdownIt添加代码高亮插件
export function setupHighlightPlugin(md: MarkdownIt) {
  md.use(require('markdown-it-highlightjs'), { 
    hljs,
    auto: true,
    code: true
  })
}

// 为MarkdownIt添加Diff显示插件
export function setupDiffPlugin(md: MarkdownIt) {
  // 自定义渲染函数来处理差异显示
  const originalRender = md.renderer.rules.fence || ((tokens, idx, options, env, renderer) => {
    return renderer.renderToken(tokens, idx, options)
  })

  md.renderer.rules.fence = (tokens, idx, options, env, renderer) => {
    const token = tokens[idx]
    const info = token.info ? md.utils.unescapeAll(token.info).trim() : ''
    let langName = ''
    
    if (info) {
      langName = info.split(/\s+/g)[0]
    }

    if (langName === 'diff') {
      // 处理diff格式的内容
      const content = token.content
      
      // 解析diff内容
      const lines = content.split('\n')
      let result = '<div class="diff-container"><pre><code class="language-diff">'
      
      lines.forEach(line => {
        if (line.startsWith('+')) {
          result += `<span class="diff-added">${line}</span>\n`
        } else if (line.startsWith('-')) {
          result += `<span class="diff-removed">${line}</span>\n`
        } else if (line.startsWith('@@')) {
          result += `<span class="diff-meta">${line}</span>\n`
        } else {
          result += `${line}\n`
        }
      })
      
      result += '</code></pre></div>'
      return result
    }
    
    return originalRender(tokens, idx, options, env, renderer)
  }
}

// 创建带插件的MarkdownIt实例
export function createMarkdownIt() {
  const md = new MarkdownIt({
    html: false,
    linkify: true,
    typographer: true,
    breaks: true
  })
  
  setupHighlightPlugin(md)
  setupDiffPlugin(md)
  
  return md
}