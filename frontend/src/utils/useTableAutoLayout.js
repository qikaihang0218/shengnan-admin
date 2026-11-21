import { onBeforeUnmount, onMounted, nextTick, ref, unref } from 'vue'

/**
 * el-table 固定列在容器宽度变化后不会自动重排，会出现固定列错位遮挡相邻列。
 * 这里监听容器尺寸变化：既返回实时宽度供列显隐使用，也主动调用 doLayout 修正固定列。
 * @param {import('vue').Ref} tableRef el-table 实例
 * @param {import('vue').Ref} containerRef 表格外层容器
 */
export function useTableAutoLayout(tableRef, containerRef) {
  const width = ref(0)
  let observer = null
  let raf = 0

  const relayout = () => {
    cancelAnimationFrame(raf)
    raf = requestAnimationFrame(() => {
      const table = unref(tableRef)
      if (!table) return
      table.doLayout?.()
      // doLayout 后固定列仍需一次重绘才能对齐
      nextTick(() => table.doLayout?.())
    })
  }

  onMounted(() => {
    const el = unref(containerRef)
    if (!el) return
    width.value = el.clientWidth
    if (typeof ResizeObserver !== 'undefined') {
      observer = new ResizeObserver(entries => {
        const w = entries[0]?.contentRect.width
        if (typeof w === 'number') width.value = w
        relayout()
      })
      observer.observe(el)
    }
    window.addEventListener('resize', relayout)
    relayout()
  })

  onBeforeUnmount(() => {
    cancelAnimationFrame(raf)
    observer?.disconnect()
    window.removeEventListener('resize', relayout)
  })

  return { relayout, width }
}