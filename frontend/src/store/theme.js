import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

// 可选主题：name 显示名 / key 持久化值 / primary 主色 / shade 主色深阶(用于侧边栏/登录背景)
export const THEMES = [
  { key: 'ocean',    name: '深海蓝', primary: '#1e3c72', shades: ['#3a5f96', '#33568a', '#2c4d7d', '#254470', '#1e3c72'] },
  { key: 'forest',   name: '森林绿', primary: '#2e7d5b', shades: ['#52a07e', '#47926f', '#3d8465', '#36775a', '#2e7d5b'] },
  { key: 'purple',   name: '典雅紫', primary: '#6d4aa8', shades: ['#9071c0', '#8363b5', '#7656aa', '#724fae', '#6d4aa8'] },
  { key: 'orange',   name: '暖阳橙', primary: '#c2571a', shades: ['#d97d44', '#d17135', '#c76628', '#c55c21', '#c2571a'] },
  { key: 'dark',     name: '暗夜黑', primary: '#409eff', shades: [] }
]

const STORAGE_KEY = 'shengnan_theme'

export const useThemeStore = defineStore('theme', () => {
  const saved = localStorage.getItem(STORAGE_KEY) || 'ocean'
  const current = ref(THEMES.some(t => t.key === saved) ? saved : 'ocean')
  const isDark = ref(current.value === 'dark')

  function apply(key) {
    const t = THEMES.find(x => x.key === key) || THEMES[0]
    current.value = t.key
    isDark.value = t.key === 'dark'
    const root = document.documentElement
    root.classList.toggle('dark', isDark.value)
    root.setAttribute('data-theme', t.key)

    // 覆盖 Element Plus 主色变量：主色 + 5 个浅阶 + 3 个深阶
    root.style.setProperty('--el-color-primary', t.primary)
    for (let i = 1; i <= 5; i++) {
      root.style.setProperty('--el-color-primary-light-' + i, mixWithWhite(t.primary, i * 0.2))
    }
    for (let i = 1; i <= 3; i++) {
      root.style.setProperty('--el-color-primary-dark-' + i, mixWithBlack(t.primary, i * 0.1))
    }
    // 布局自定义变量（侧边栏/登录背景/顶栏）
    root.style.setProperty('--sn-side-bg', t.shades.length ? t.shades[4] : '#1b1f27')
    root.style.setProperty('--sn-side-text', isDark.value ? '#c8d4e8' : '#c8d4e8')
    root.style.setProperty('--sn-side-active', '#ffffff')
    root.style.setProperty('--sn-brand', t.primary)
    root.style.setProperty('--sn-brand-2', t.shades.length ? t.shades[0] : (isDark.value ? '#5a6577' : t.primary))
    root.style.setProperty('--sn-logo-color', isDark.value ? '#ffffff' : t.primary)

    localStorage.setItem(STORAGE_KEY, t.key)
  }

  // 应用启动时恢复上次选择
  apply(current.value)
  watch(current, v => apply(v))

  return { current, isDark, apply }
})

function hexToRgb(hex) {
  const h = hex.replace('#', '')
  return [parseInt(h.substring(0, 2), 16), parseInt(h.substring(2, 4), 16), parseInt(h.substring(4, 6), 16)]
}
function mixWithWhite(hex, weight) {
  const [r, g, b] = hexToRgb(hex)
  return 'rgb(' + [r, g, b].map(c => Math.round(c + (255 - c) * weight)).join(',') + ')'
}
function mixWithBlack(hex, weight) {
  const [r, g, b] = hexToRgb(hex)
  return 'rgb(' + [r, g, b].map(c => Math.round(c * (1 - weight))).join(',') + ')'
}
