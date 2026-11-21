<template>
  <!-- html 来自 renderSafeMarkdown：已整体转义 + 白名单标签生成，无可执行的原始 HTML -->
  <div class="safe-markdown" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue'
import { renderSafeMarkdown } from '../utils/safeMarkdown'

const props = defineProps({
  content: { type: String, default: '' }
})

const html = computed(() => renderSafeMarkdown(props.content))
</script>

<style>
.safe-markdown { line-height: 1.7; word-break: break-word; }
.safe-markdown p { margin: 0 0 8px; }
.safe-markdown p:last-child { margin-bottom: 0; }
.safe-markdown h1, .safe-markdown h2, .safe-markdown h3, .safe-markdown h4 {
  margin: 12px 0 6px; font-weight: 600; line-height: 1.4;
}
.safe-markdown h1 { font-size: 20px; }
.safe-markdown h2 { font-size: 18px; }
.safe-markdown h3 { font-size: 16px; }
.safe-markdown h4 { font-size: 14px; }
.safe-markdown ul, .safe-markdown ol { margin: 6px 0 8px; padding-left: 22px; }
.safe-markdown li { margin: 2px 0; }
.safe-markdown blockquote {
  margin: 8px 0; padding: 6px 12px; border-left: 3px solid var(--el-color-primary, #409eff);
  background: var(--el-fill-color-light, #f5f7fa); color: var(--el-text-color-regular, #606266);
}
.safe-markdown code {
  font-family: "SFMono-Regular", Consolas, monospace; font-size: 12px;
  background: var(--el-fill-color, #f4f4f5); padding: 2px 6px; border-radius: 3px;
  color: var(--el-color-primary, #409eff);
}
.safe-markdown pre {
  margin: 8px 0; padding: 10px 12px; border-radius: 6px; overflow-x: auto;
  background: var(--el-fill-color, #f4f4f5);
}
.safe-markdown pre code { background: transparent; padding: 0; color: var(--el-text-color-primary, #303133); }
.safe-markdown a { color: var(--el-color-primary, #409eff); text-decoration: none; }
.safe-markdown a:hover { text-decoration: underline; }
.safe-markdown hr { border: 0; border-top: 1px solid var(--el-border-color, #dcdfe6); margin: 12px 0; }
.safe-markdown del { color: var(--el-text-color-secondary, #909399); }
</style>
