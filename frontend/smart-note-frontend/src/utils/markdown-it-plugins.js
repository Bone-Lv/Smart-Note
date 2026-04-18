import MarkdownIt from 'markdown-it'

// 禁用 HTML，只渲染纯 Markdown
const md = new MarkdownIt({
  html: false,
  linkify: true
})

export function renderMarkdown(content) {
  return md.render(content || '')
}