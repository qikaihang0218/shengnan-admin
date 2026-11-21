import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

/**
 * 配色方案
 * key        持久化标识
 * primary    主色（驱动 Element Plus 主色阶）
 * gradient   侧栏渐变基准（起 → 止）
 * sideText   侧栏常规文字
 * sideActive 侧栏选中文字
 * side       侧栏基准明暗（light 时 Logo 反色）
 * page       内容区明暗（dark 时挂载 dark 类）
 */
export const THEMES = [
  { key: 'ocean',    name: '深海蓝', desc: '沉稳商务', primary: '#2a5298', gradient: ['#16294d', '#2a5298'], sideText: '#c3d0e8', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'forest',   name: '森林绿', desc: '自然清新', primary: '#2e7d5b', gradient: ['#14432f', '#3f9e78'], sideText: '#c6e3d6', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'purple',   name: '典雅紫', desc: '高贵优雅', primary: '#7e5cc0', gradient: ['#2f1c52', '#7e5cc0'], sideText: '#d8cdf0', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'sunset',   name: '暖阳橙', desc: '温暖活力', primary: '#d97a2b', gradient: ['#6d3210', '#e08a3c'], sideText: '#f4dcc6', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'rose',     name: '玫瑰红', desc: '柔美知性', primary: '#c2185b', gradient: ['#6b0f34', '#d94f83'], sideText: '#f2cfdd', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'cyan',     name: '科技青', desc: '冷静理性', primary: '#0f8b8d', gradient: ['#053c3e', '#17a2a5'], sideText: '#c2e8e9', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'graphite', name: '石墨灰', desc: '极简克制', primary: '#5b6b82', gradient: ['#1c222c', '#414d5f'], sideText: '#c2c9d4', sideActive: '#ffffff', side: 'dark',  page: 'light' },
  { key: 'light',    name: '极简白', desc: '明亮通透', primary: '#2563eb', gradient: ['#ffffff', '#eef2fb'], sideText: '#4b5563', sideActive: '#2563eb', side: 'light', page: 'light' },
  { key: 'dark',     name: '暗夜黑', desc: '深色护眼', primary: '#409eff', gradient: ['#0d1117', '#1b2432'], sideText: '#a9b6c9', sideActive: '#ffffff', side: 'dark',  page: 'dark' }
]

/** 侧栏样式 */
export const SIDE_STYLES = [
  { key: 'gradient', name: '渐变', desc: '双色渐变背景' },
  { key: 'solid',    name: '纯色', desc: '单色扁平' },
  { key: 'light',    name: '浅色', desc: '明亮白底' },
  { key: 'brand',    name: '品牌条', desc: '浅底 + 品牌色边' }
]

/** 顶栏样式 */
export const HEADER_STYLES = [
  { key: 'glass',  name: '毛玻璃', desc: '半透明模糊' },
  { key: 'plain',  name: '纯色', desc: '简洁平面' },
  { key: 'brand',  name: '品牌渐变', desc: '主色渐变' },
  { key: 'accent', name: '描边', desc: '主色下边框' }
]

const KEY_THEME = 'shengnan_theme'
const KEY_SIDE = 'shengnan_side_style'
const KEY_HEADER = 'shengnan_header_style'

function pick(list, key, fallback) {
  return list.some(x => x.key === key) ? key : fallback
}

export const useThemeStore = defineStore('theme', () => {
  const current = ref(pick(THEMES, localStorage.getItem(KEY_THEME), 'ocean'))
  const sideStyle = ref(pick(SIDE_STYLES, localStorage.getItem(KEY_SIDE), 'gradient'))
  const headerStyle = ref(pick(HEADER_STYLES, localStorage.getItem(KEY_HEADER), 'glass'))
  const isDark = ref(false)

  const meta = computed(() => THEMES.find(t => t.key === current.value) || THEMES[0])
  const name = computed(() => meta.value.name)
  const sideMeta = computed(() => SIDE_STYLES.find(s => s.key === sideStyle.value) || SIDE_STYLES[0])
  const headerMeta = computed(() => HEADER_STYLES.find(s => s.key === headerStyle.value) || HEADER_STYLES[0])

  function render() {
    const t = meta.value
    const root = document.documentElement
    isDark.value = t.page === 'dark'

    root.classList.toggle('dark', isDark.value)
    root.setAttribute('data-theme', t.key)
    root.setAttribute('data-side-style', sideStyle.value)
    root.setAttribute('data-header-style', headerStyle.value)
    root.setAttribute('data-page', t.page)

    // Element Plus 主色阶：light-i = i×10% 白，dark-i = i×10% 黑
    root.style.setProperty('--el-color-primary', t.primary)
    for (let i = 1; i <= 9; i++) {
      root.style.setProperty('--el-color-primary-light-' + i, mixWithWhite(t.primary, i * 0.1))
    }
    for (let i = 1; i <= 3; i++) {
      root.style.setProperty('--el-color-primary-dark-' + i, mixWithBlack(t.primary, i * 0.1))
    }

    // 品牌色
    root.style.setProperty('--sn-brand', t.primary)
    root.style.setProperty('--sn-brand-2', t.gradient[1])
    root.style.setProperty('--sn-brand-dark', t.gradient[0])

    applySide(t, root)
    applyHeader(t, root)

    localStorage.setItem(KEY_THEME, t.key)
    localStorage.setItem(KEY_SIDE, sideStyle.value)
    localStorage.setItem(KEY_HEADER, headerStyle.value)
  }

  function applySide(t, root) {
    const s = sideStyle.value
    let bg1, bg2, text, active, hoverBg, activeBg, border, logoBg, logoColor, accentW, shadow

    if (s === 'light' || s === 'brand') {
      const pageDark = t.page === 'dark'
      const base = pageDark ? '#1c222c' : '#ffffff'
      bg1 = base
      bg2 = base
      text = pageDark ? '#a9b6c9' : '#5a6472'
      active = t.primary
      hoverBg = pageDark ? 'rgba(255,255,255,.07)' : 'rgba(0,0,0,.045)'
      activeBg = pageDark ? 'rgba(255,255,255,.10)' : mixWithWhite(t.primary, 0.88)
      border = pageDark ? 'rgba(255,255,255,.10)' : 'rgba(0,0,0,.07)'
      logoBg = t.primary
      logoColor = '#ffffff'
      accentW = s === 'brand' ? '3px' : '0px'
      shadow = pageDark ? '2px 0 12px rgba(0,0,0,.35)' : '2px 0 12px rgba(0,0,0,.06)'
    } else {
      bg1 = t.gradient[0]
      bg2 = s === 'solid' ? t.gradient[0] : t.gradient[1]
      text = t.sideText
      active = t.sideActive
      hoverBg = 'rgba(255,255,255,.09)'
      activeBg = 'rgba(255,255,255,.17)'
      border = 'rgba(255,255,255,.10)'
      logoBg = '#ffffff'
      logoColor = t.primary
      accentW = '0px'
      shadow = '2px 0 12px rgba(0,0,0,.14)'
    }

    root.style.setProperty('--sn-side-bg', bg1)
    root.style.setProperty('--sn-side-bg-2', bg2)
    root.style.setProperty('--sn-side-text', text)
    root.style.setProperty('--sn-side-active', active)
    root.style.setProperty('--sn-side-hover-bg', hoverBg)
    root.style.setProperty('--sn-side-active-bg', activeBg)
    root.style.setProperty('--sn-side-border', border)
    root.style.setProperty('--sn-side-shadow', shadow)
    root.style.setProperty('--sn-side-accent-w', accentW)
    root.style.setProperty('--sn-side-edge', s === 'brand' ? '3px solid ' + t.primary : '0 solid transparent')
    root.style.setProperty('--sn-logo-bg', logoBg)
    root.style.setProperty('--sn-logo-color', logoColor)
    // 侧栏高光叠加：仅渐变样式启用
    root.style.setProperty('--sn-side-glow', s === 'gradient' ? '0.10' : '0')
  }

  function applyHeader(t, root) {
    const h = headerStyle.value
    const pageDark = t.page === 'dark'
    const solidBg = pageDark ? '#1c222c' : '#ffffff'
    let bg, color, border, shadow, blur

    if (h === 'brand') {
      bg = 'linear-gradient(90deg, ' + t.primary + ' 0%, ' + t.gradient[1] + ' 100%)'
      color = '#ffffff'
      border = 'transparent'
      shadow = '0 2px 12px ' + mixWithBlack(t.primary, 0.3) + '66'
      blur = 'none'
    } else if (h === 'accent') {
      bg = solidBg
      color = pageDark ? '#d7dee8' : '#4b5563'
      border = t.primary
      shadow = 'none'
      blur = 'none'
    } else if (h === 'plain') {
      bg = solidBg
      color = pageDark ? '#d7dee8' : '#4b5563'
      border = pageDark ? 'rgba(255,255,255,.10)' : 'rgba(0,0,0,.07)'
      shadow = '0 1px 4px rgba(0,0,0,.04)'
      blur = 'none'
    } else {
      bg = pageDark ? 'rgba(28,34,44,.85)' : 'rgba(255,255,255,.82)'
      color = pageDark ? '#d7dee8' : '#4b5563'
      border = pageDark ? 'rgba(255,255,255,.10)' : 'rgba(0,0,0,.06)'
      shadow = '0 1px 8px rgba(0,0,0,.05)'
      blur = 'blur(12px)'
    }

    const hoverBg = h === 'brand' ? 'rgba(255,255,255,.18)' : (pageDark ? 'rgba(255,255,255,.08)' : 'rgba(0,0,0,.045)')

    root.style.setProperty('--sn-header-bg', bg)
    root.style.setProperty('--sn-header-color', color)
    root.style.setProperty('--sn-header-border', border)
    root.style.setProperty('--sn-header-shadow', shadow)
    root.style.setProperty('--sn-header-blur', blur)
    root.style.setProperty('--sn-header-hover-bg', hoverBg)
    root.style.setProperty('--sn-header-border-w', h === 'accent' ? '2px' : '1px')
  }

  function apply(key) {
    current.value = pick(THEMES, key, 'ocean')
    render()
  }
  function setSideStyle(key) {
    sideStyle.value = pick(SIDE_STYLES, key, 'gradient')
    render()
  }
  function setHeaderStyle(key) {
    headerStyle.value = pick(HEADER_STYLES, key, 'glass')
    render()
  }
  function reset() {
    current.value = 'ocean'
    sideStyle.value = 'gradient'
    headerStyle.value = 'glass'
    render()
  }

  render()

  return {
    current, sideStyle, headerStyle, isDark,
    meta, name, sideMeta, headerMeta,
    apply, setSideStyle, setHeaderStyle, reset
  }
})

function hexToRgb(hex) {
  const h = hex.replace('#', '')
  return [parseInt(h.substring(0, 2), 16), parseInt(h.substring(2, 4), 16), parseInt(h.substring(4, 6), 16)]
}
export function mixWithWhite(hex, weight) {
  const [r, g, b] = hexToRgb(hex)
  return 'rgb(' + [r, g, b].map(c => Math.round(c + (255 - c) * weight)).join(',') + ')'
}
export function mixWithBlack(hex, weight) {
  const [r, g, b] = hexToRgb(hex)
  return 'rgb(' + [r, g, b].map(c => Math.round(c * (1 - weight))).join(',') + ')'
}