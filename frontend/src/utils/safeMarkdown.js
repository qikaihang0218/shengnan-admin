/**
 * 安全的 Markdown 子集渲染。
 *
 * 设计原则（防 XSS）：
 * 1. 先把所有输入做 HTML 实体转义 —— 任何 <script>、onerror、<img onerror> 都变成纯文本
 * 2. 再基于「转义后的文本」识别有限的 Markdown 语法，生成固定白名单标签
 * 3. 链接仅允许 http/https/mailto，且强制 target=_blank + rel=noopener noreferrer
 * 4. 不解析原始 HTML，不支持内联样式、script、iframe、事件属性
 *
 * 支持的语法：# 标题、**粗体**、*斜体*、`行内代码`、```代码块```、
 *            - 列表、1. 有序列表、> 引用、[文字](链接)、--- 分隔线、换行
 */

function escapeHtml(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** 链接协议白名单：只允许安全协议，其余一律丢弃链接仅保留文字 */
function safeLink(label, rawUrl) {
  const url = String(rawUrl).trim()
  if (/^(https?:\/\/|mailto:)/i.test(url)) {
    return `<a href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${label}</a>`
  }
  return label
}

/** 行内语法：输入必须是已转义的文本 */
function inline(text) {
  let s = text
  // 行内代码优先处理，避免内部内容被后续规则解析
  s = s.replace(/`([^`]+)`/g, (_, code) => `<code>${code}</code>`)
  // 链接 [label](url)
  s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, url) => safeLink(label, url))
  // 粗体 / 斜体 / 删除线
  s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  s = s.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
  s = s.replace(/~~([^~]+)~~/g, '<del>$1</del>')
  return s
}

export function renderSafeMarkdown(input) {
  if (!input) return ''
  // 第 1 步：整体转义，杜绝一切原始 HTML 注入
  const escaped = escapeHtml(input)
  const lines = escaped.split(/\r?\n/)
  const out = []

  let inCode = false
  let codeLines = []
  let listType = null // 'ul' | 'ol'
  let paragraphs = []

  const flushParagraph = () => {
    if (paragraphs.length) {
      out.push(`<p>${paragraphs.map(inline).join('<br>')}</p>`)
      paragraphs = []
    }
  }
  const closeList = () => {
    if (listType) { out.push(`</${listType}>`); listType = null }
  }

  for (const line of lines) {
    const trimmed = line.trim()

    // 代码块开合
    if (/^```/.test(trimmed)) {
      flushParagraph(); closeList()
      if (!inCode) { inCode = true; codeLines = [] }
      else { inCode = false; out.push(`<pre><code>${codeLines.join('\n')}</code></pre>`) }
      continue
    }
    if (inCode) { codeLines.push(line); continue }

    // 空行：结束段落与列表
    if (!trimmed) { flushParagraph(); closeList(); continue }

    // 标题（h1-h4）
    const heading = trimmed.match(/^(#{1,4})\s+(.*)$/)
    if (heading) {
      flushParagraph(); closeList()
      const level = heading[1].length
      out.push(`<h${level}>${inline(heading[2])}</h${level}>`)
      continue
    }

    // 分隔线
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      flushParagraph(); closeList()
      out.push('<hr>')
      continue
    }

    // 引用
    const quote = trimmed.match(/^&gt;\s?(.*)$/)
    if (quote) {
      flushParagraph(); closeList()
      out.push(`<blockquote>${inline(quote[1])}</blockquote>`)
      continue
    }

    // 无序列表
    const ul = trimmed.match(/^[-+]\s+(.*)$/)
    if (ul) {
      flushParagraph()
      if (listType !== 'ul') { closeList(); out.push('<ul>'); listType = 'ul' }
      out.push(`<li>${inline(ul[1])}</li>`)
      continue
    }

    // 有序列表
    const ol = trimmed.match(/^\d+\.\s+(.*)$/)
    if (ol) {
      flushParagraph()
      if (listType !== 'ol') { closeList(); out.push('<ol>'); listType = 'ol' }
      out.push(`<li>${inline(ol[1])}</li>`)
      continue
    }

    // 普通文本段落
    if (listType) closeList()
    paragraphs.push(trimmed)
  }

  flushParagraph(); closeList()
  if (inCode && codeLines.length) out.push(`<pre><code>${codeLines.join('\n')}</code></pre>`)
  return out.join('')
}
