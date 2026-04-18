// markdown.worker.js - Web Worker for Markdown rendering
import md from './markdownRenderer.js';

self.onmessage = (e) => {
  const { content, id } = e.data;
  
  try {
    const result = md.render(content);
    self.postMessage({ id, result, error: null });
  } catch (error) {
    self.postMessage({ id, result: null, error: error.message });
  }
};
