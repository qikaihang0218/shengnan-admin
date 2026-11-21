<template>
  <el-container class="layout">
    <div v-if="isMobile && mobileOpen" class="aside-mask" @click="mobileOpen = false"></div>
    <el-aside :width="asideWidth" class="aside" :class="{ mobile: isMobile, open: mobileOpen }">
      <div class="logo" @click="toggleAside">
        <span class="logo-mark"><LeafLogo /></span>
        <span v-show="isMobile || !collapsed" class="logo-text">胜楠管理平台</span>
      </div>
      <el-scrollbar>
        <el-menu :default-active="route.path" :collapse="!isMobile && collapsed" router
                 background-color="transparent" text-color="var(--sn-side-text)" active-text-color="var(--sn-side-active)">
          <template v-for="node in menus" :key="node.id">
            <el-sub-menu v-if="node.children && node.children.length" :index="'sub-' + node.id">
              <template #title>
                <el-icon><component :is="iconOf(node.icon)" /></el-icon>
                <span>{{ node.name }}</span>
              </template>
              <el-menu-item v-for="child in node.children" :key="child.id" :index="childIndex(node, child)">
                <el-icon><component :is="iconOf(child.icon)" /></el-icon>
                <span>{{ child.name }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="node.path">
              <el-icon><component :is="iconOf(node.icon)" /></el-icon>
              <span>{{ node.name }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="content-container">
      <el-header class="header">
        <div class="header-left">
          <el-icon class="fold-btn" @click="toggleAside">
            <component :is="(isMobile ? mobileOpen : !collapsed) ? 'Fold' : 'Expand'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag size="small" type="info">{{ scopeLabel }}</el-tag>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notice-badge">
            <el-icon class="notice-bell" size="20" @click="openNotices"><Bell /></el-icon>
          </el-badge>
          <span class="theme-trigger" @click="themeVisible = true">
            <el-icon><Brush /></el-icon>
            <span class="theme-name">{{ themeStore.name }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <el-dropdown @command="handleCommand">
            <span class="user-trigger">
              <el-icon><UserFilled /></el-icon>
              {{ store.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 主题选择 -->
  <el-dialog v-model="themeVisible" title="外观设置" width="min(720px, 94vw)" class="theme-dialog">
    <div class="theme-section">
      <div class="section-title">
        <span>配色方案</span>
        <span class="section-hint">{{ themeStore.name }}</span>
      </div>
      <div class="theme-grid">
        <div v-for="t in themes" :key="t.key" class="theme-card"
             :class="{ active: t.key === themeStore.current }" @click="themeStore.apply(t.key)">
          <div class="theme-preview" :style="{ background: `linear-gradient(135deg, ${t.gradient[0]}, ${t.gradient[1]})` }">
            <span class="tp-bar" :style="{ background: t.side === 'light' ? 'rgba(0,0,0,.10)' : 'rgba(255,255,255,.22)' }"></span>
            <span class="tp-bar short" :style="{ background: t.side === 'light' ? 'rgba(0,0,0,.07)' : 'rgba(255,255,255,.15)' }"></span>
            <span class="tp-dot" :style="{ background: t.primary }"></span>
          </div>
          <div class="theme-meta">
            <div class="theme-name">
              {{ t.name }}
              <el-icon v-if="t.key === themeStore.current" class="theme-check"><Check /></el-icon>
            </div>
            <div class="theme-desc">{{ t.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="theme-section">
      <div class="section-title">
        <span>侧栏样式</span>
        <span class="section-hint">{{ themeStore.sideMeta.name }}</span>
      </div>
      <div class="style-row">
        <div v-for="s in sideStyles" :key="s.key" class="style-chip"
             :class="{ active: s.key === themeStore.sideStyle }" @click="themeStore.setSideStyle(s.key)">
          <span class="style-swatch" :class="'sw-side-' + s.key"></span>
          <span class="style-text">
            <b>{{ s.name }}</b>
            <small>{{ s.desc }}</small>
          </span>
          <el-icon v-if="s.key === themeStore.sideStyle" class="style-check"><Check /></el-icon>
        </div>
      </div>
    </div>

    <div class="theme-section">
      <div class="section-title">
        <span>顶栏样式</span>
        <span class="section-hint">{{ themeStore.headerMeta.name }}</span>
      </div>
      <div class="style-row">
        <div v-for="h in headerStyles" :key="h.key" class="style-chip"
             :class="{ active: h.key === themeStore.headerStyle }" @click="themeStore.setHeaderStyle(h.key)">
          <span class="style-swatch" :class="'sw-head-' + h.key"></span>
          <span class="style-text">
            <b>{{ h.name }}</b>
            <small>{{ h.desc }}</small>
          </span>
          <el-icon v-if="h.key === themeStore.headerStyle" class="style-check"><Check /></el-icon>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button link @click="themeStore.reset()">恢复默认</el-button>
      <el-button type="primary" @click="themeVisible = false">完成</el-button>
    </template>
  </el-dialog>

  <!-- 全员通知抽屉 -->
  <el-drawer v-model="noticeVisible" title="通知公告" size="min(420px, 90vw)">
    <div class="notice-toolbar">
      <el-button link type="primary" :disabled="unreadCount === 0" @click="readAll">全部标为已读</el-button>
      <el-button link @click="loadNotices">刷新</el-button>
    </div>
    <div v-if="notices.length === 0" class="notice-empty">暂无通知</div>
    <div v-for="n in notices" :key="n.id" class="notice-item" :class="{ unread: !n.read }" @click="readOne(n)">
      <div class="notice-item-head">
        <el-tag :type="n.noticeType === 2 ? 'success' : 'info'" size="small">{{ n.typeName || (n.noticeType === 2 ? '公告' : '通知') }}</el-tag>
        <el-tag v-if="n.isTop" type="warning" size="small">置顶</el-tag>
        <span v-if="!n.read" class="notice-dot"></span>
        <span class="notice-time">{{ n.publishTime || n.createdAt }}</span>
      </div>
      <div class="notice-title">{{ n.title }}</div>
      <div class="notice-content"><SafeMarkdown :content="n.content" /></div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { useThemeStore, THEMES, SIDE_STYLES, HEADER_STYLES } from '../store/theme'
import request from '../utils/request'
import LeafLogo from '../components/LeafLogo.vue'
import SafeMarkdown from '../components/SafeMarkdown.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const themeStore = useThemeStore()
const themes = THEMES
const sideStyles = SIDE_STYLES
const headerStyles = HEADER_STYLES
const themeVisible = ref(false)
const collapsed = ref(false)
const isMobile = ref(false)
const mobileOpen = ref(false)
const asideWidth = computed(() => (isMobile.value ? '220px' : collapsed.value ? '64px' : '220px'))

function onResize() {
  const w = window.innerWidth
  const mobile = w < 768
  if (mobile !== isMobile.value) {
    isMobile.value = mobile
    mobileOpen.value = false
    collapsed.value = false
  }
}
function toggleAside() {
  if (isMobile.value) mobileOpen.value = !mobileOpen.value
  else collapsed.value = !collapsed.value
}
onMounted(() => { isMobile.value = window.innerWidth < 768; window.addEventListener('resize', onResize) })
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
const menus = computed(() => store.routes.filter(r => r.type !== 'action' && r.visible !== false))

// 图标名 → Element Plus 组件名（后端存 PascalCase，直接可用）
function iconOf(name) { return name || 'Menu' }

// 子菜单完整路径：/system + account → /system/account
function childIndex(node, child) {
  const base = node.path.startsWith('/') ? node.path : '/' + node.path
  const sub = child.path.startsWith('/') ? child.path.slice(1) : child.path
  return base + '/' + sub
}

const scopeLabel = computed(() => ({
  all: '全部数据', dept: '本部门', dept_tree: '本部门及以下', self: '仅本人'
}[store.dataScope] || '仅本人'))

function handleCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}

/* ---------- 全员通知 ---------- */
const noticeVisible = ref(false)
const notices = ref([])
const unreadCount = ref(0)

async function loadUnread() {
  try { const res = await request.get('/notice/unread-count'); unreadCount.value = res.data || 0 }
  catch (e) { /* 通知未就绪时不影响主界面 */ }
}
async function loadNotices() {
  try { const res = await request.get('/notice/published'); notices.value = res.data || [] }
  catch (e) { /* ignore */ }
}
function openNotices() { noticeVisible.value = true; loadNotices() }
async function readOne(n) {
  if (n.read) return
  try {
    await request.post('/notice/read/' + n.id)
    n.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) { /* ignore */ }
}
async function readAll() {
  try {
    await request.post('/notice/read-all')
    notices.value.forEach(n => { n.read = true })
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch (e) { /* ignore */ }
}
onMounted(loadUnread)
</script>

<style scoped>
.layout { height: 100vh; overflow: hidden; }
.aside {
  background: linear-gradient(180deg, var(--sn-side-bg) 0%, var(--sn-side-bg-2) 100%);
  overflow: hidden; transition: width .2s, transform .2s, background .25s;
  box-shadow: var(--sn-side-shadow, 2px 0 12px rgba(0,0,0,.10));
  position: relative;
  display: flex; flex-direction: column;
  flex-shrink: 0;
}
.aside.mobile {
  position: fixed; left: 0; top: 0; bottom: 0; z-index: 1200;
  transform: translateX(-100%);
  box-shadow: 4px 0 24px rgba(0,0,0,.25);
}
.aside.mobile.open { transform: translateX(0); }
.aside-mask {
  position: fixed; inset: 0; z-index: 1199;
  background: rgba(0,0,0,.45);
  backdrop-filter: blur(2px);
}
.aside > :deep(.el-scrollbar) { flex: 1 1 auto; min-height: 0; }
.aside :deep(.el-scrollbar__view) { padding-bottom: 8px; }
/* 品牌条样式：右侧品牌色描边（由 --sn-side-edge 驱动） */
.aside { border-right: var(--sn-side-edge, 0 solid transparent); }
.aside::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(120% 60% at 0% 0%, rgba(255,255,255,var(--sn-side-glow, .10)), transparent 60%);
}
.content-container { min-width: 0; overflow: hidden; background: var(--el-bg-color-page); }
.logo {
  height: 60px; display: flex; align-items: center; gap: 10px;
  padding: 0 16px; cursor: pointer; position: relative; z-index: 1; flex: 0 0 60px;
  border-bottom: 1px solid var(--sn-side-border);
  color: var(--sn-side-active); overflow: hidden; white-space: nowrap;
}
.logo-mark {
  width: 32px; height: 32px; padding: 6px;
  background: var(--sn-logo-bg); color: var(--sn-logo-color);
  border-radius: 9px; font-weight: bold; flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,.15);
}
.logo-text { font-size: 16px; font-weight: 600; letter-spacing: .5px; }
.aside :deep(.el-menu) { border-right: none; background: transparent !important; }
.aside :deep(.el-menu-item),
.aside :deep(.el-sub-menu__title) {
  margin: 4px 10px; border-radius: 8px; height: 44px; line-height: 44px;
  transition: background .18s, color .18s;
}
.aside :deep(.el-menu-item:hover),
.aside :deep(.el-sub-menu__title:hover) { background: var(--sn-side-hover-bg) !important; }
.aside :deep(.el-menu-item.is-active) {
  background: var(--sn-side-active-bg) !important;
  color: var(--sn-side-active) !important; font-weight: 600;
  box-shadow: inset var(--sn-side-accent-w, 3px) 0 0 var(--sn-side-active);
}
.aside :deep(.el-sub-menu .el-menu-item) { margin: 2px 10px 2px 18px; height: 40px; line-height: 40px; }
.header {
  height: 60px; flex: 0 0 60px; display: flex;
  align-items: center; justify-content: space-between;
  gap: 12px; padding: 0 16px;
  background: var(--sn-header-bg);
  backdrop-filter: var(--sn-header-blur, blur(10px));
  color: var(--sn-header-color, var(--el-text-color-regular));
  border-bottom: var(--sn-header-border-w, 1px) solid var(--sn-header-border, var(--el-border-color-lighter));
  box-shadow: var(--sn-header-shadow, 0 1px 8px rgba(0,0,0,.04));
  transition: background .25s, color .25s;
}
.header-left, .header-right { display: flex; align-items: center; gap: 16px; min-width: 0; }
.header-left { flex: 1 1 auto; overflow: hidden; }
.header-right { flex: 0 0 auto; }
.header :deep(.el-breadcrumb) { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.header :deep(.el-breadcrumb__inner),
.header :deep(.el-breadcrumb__separator) { color: var(--sn-header-color, var(--el-text-color-regular)) !important; }
.theme-trigger {
  display: flex; align-items: center; gap: 6px; cursor: pointer;
  color: inherit; padding: 5px 10px; border-radius: 8px;
  transition: background .18s;
}
.theme-trigger:hover { background: var(--sn-header-hover-bg, var(--el-fill-color-light)); }
.fold-btn { font-size: 20px; cursor: pointer; color: inherit; }
.header :deep(.el-dropdown) { color: inherit; }
.user-trigger { display: flex; align-items: center; gap: 6px; cursor: pointer; color: inherit; }
.fold-btn:hover, .user-trigger:hover, .notice-bell:hover { opacity: .75; }
.main { padding: 20px; overflow-y: auto; overflow-x: hidden; }
.notice-bell { cursor: pointer; color: inherit; }

.notice-toolbar { display: flex; justify-content: flex-end; gap: 12px; margin-bottom: 12px; }
.notice-empty { color: #909399; text-align: center; padding: 40px 0; }
.notice-item {
  padding: 12px 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px;
  margin-bottom: 10px; cursor: pointer; background: var(--el-bg-color);
}
.notice-item.unread { background: var(--el-color-primary-light-9); border-color: var(--el-color-primary-light-7); }
.notice-item-head { display: flex; align-items: center; gap: 6px; }
.notice-time { margin-left: auto; color: var(--el-text-color-secondary); font-size: 12px; }
.notice-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--el-color-danger); }
.notice-title { font-weight: 600; margin: 8px 0 4px; color: var(--el-text-color-primary); }
.notice-content { color: var(--el-text-color-regular); font-size: 13px; line-height: 1.6; white-space: pre-wrap; }

/* ---------- 响应式（须置于末尾以覆盖基础样式） ---------- */
@media (max-width: 1200px) {
  .main { padding: 16px; }
  .header-left, .header-right { gap: 12px; }
  .theme-trigger { padding: 5px 8px; }
}
@media (max-width: 992px) {
  .theme-name { display: none; }
  .header :deep(.el-breadcrumb) { display: none; }
}
@media (max-width: 768px) {
  .main { padding: 12px; }
  .header { padding: 0 10px; gap: 8px; }
  .header-right { gap: 8px; }
  .user-trigger { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
@media (max-width: 480px) {
  .header-right :deep(.el-tag) { display: none; }
}
</style>

<style>
/* 品牌渐变顶栏下的标签对比度 */
:root[data-header-style="brand"] .header .el-tag {
  background: rgba(255, 255, 255, .18) !important;
  border-color: rgba(255, 255, 255, .35) !important;
  color: #ffffff !important;
}
/* 外观设置弹窗（teleport 到 body，需全局作用域） */
/* 整体限高：浏览器放大/缩小或小屏时标题与底部按钮固定，中间内容区滚动 */
.theme-dialog {
  display: flex; flex-direction: column;
  margin: 5vh auto !important;
  max-height: 90vh;
}
.theme-dialog .el-dialog__header { flex: 0 0 auto; }
.theme-dialog .el-dialog__footer { flex: 0 0 auto; }
.theme-dialog .theme-section + .theme-section { margin-top: 16px; }
.theme-dialog .section-title {
  display: flex; align-items: baseline; gap: 8px;
  font-size: 13px; font-weight: 600; color: var(--el-text-color-primary);
  margin-bottom: 10px;
}
.theme-dialog .section-title::before {
  content: ''; width: 3px; height: 13px; border-radius: 2px;
  background: var(--el-color-primary); align-self: center;
}
.theme-dialog .section-hint { font-size: 12px; font-weight: 400; color: var(--el-text-color-secondary); }
.theme-dialog .theme-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.theme-dialog .theme-card {
  border: 2px solid var(--el-border-color-lighter); border-radius: 10px;
  overflow: hidden; cursor: pointer; transition: all .18s; background: var(--el-bg-color);
}
.theme-dialog .theme-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,.12); }
.theme-dialog .theme-card.active { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-8); }
.theme-dialog .theme-preview { height: 44px; padding: 9px; display: flex; flex-direction: column; gap: 5px; position: relative; }
.theme-dialog .tp-bar { display: block; height: 5px; width: 58%; border-radius: 3px; }
.theme-dialog .tp-bar.short { width: 36%; }
.theme-dialog .tp-dot { position: absolute; right: 8px; bottom: 8px; width: 13px; height: 13px; border-radius: 50%; border: 2px solid rgba(255,255,255,.85); }
.theme-dialog .theme-meta { padding: 6px 10px 7px; }
.theme-dialog .theme-name { font-size: 12.5px; font-weight: 600; display: flex; align-items: center; gap: 5px; }
.theme-dialog .theme-check { color: var(--el-color-primary); font-size: 13px; }
.theme-dialog .theme-desc { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 1px; }

.theme-dialog .style-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.theme-dialog .style-chip {
  display: flex; align-items: center; gap: 9px; cursor: pointer;
  border: 2px solid var(--el-border-color-lighter); border-radius: 10px;
  padding: 9px 10px; transition: all .18s; background: var(--el-bg-color);
  position: relative; overflow: hidden;
}
.theme-dialog .style-chip:hover { border-color: var(--el-color-primary-light-5); background: var(--el-fill-color-light); }
.theme-dialog .style-chip.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.theme-dialog .style-swatch {
  width: 26px; height: 26px; border-radius: 7px; flex-shrink: 0;
  border: 1px solid var(--el-border-color-lighter);
}
.theme-dialog .sw-side-gradient { background: linear-gradient(135deg, var(--sn-brand-dark, #16294d), var(--sn-brand-2, #2a5298)); }
.theme-dialog .sw-side-solid { background: var(--sn-brand-dark, #16294d); }
.theme-dialog .sw-side-light { background: #ffffff; border-color: #d8dde5; }
.theme-dialog .sw-side-brand { background: #ffffff; border-color: #d8dde5; border-left: 4px solid var(--sn-brand, #2a5298); }
.theme-dialog .sw-head-glass { background: linear-gradient(135deg, rgba(255,255,255,.92), rgba(210,220,240,.6)); border-color: #d8dde5; }
.theme-dialog .sw-head-plain { background: #ffffff; border-color: #d8dde5; }
.theme-dialog .sw-head-brand { background: linear-gradient(90deg, var(--sn-brand, #2a5298), var(--sn-brand-2, #2a5298)); }
.theme-dialog .sw-head-accent { background: #ffffff; border-color: #d8dde5; border-bottom: 3px solid var(--sn-brand, #2a5298); }
.theme-dialog .style-text { display: flex; flex-direction: column; min-width: 0; }
.theme-dialog .style-text b { font-size: 12.5px; font-weight: 600; color: var(--el-text-color-primary); }
.theme-dialog .style-text small { font-size: 11px; color: var(--el-text-color-secondary); white-space: nowrap; }
.theme-dialog .style-check { position: absolute; right: 6px; top: 6px; color: var(--el-color-primary); font-size: 13px; }
.theme-dialog .el-dialog__body { padding: 8px 20px 4px; flex: 1 1 auto; min-height: 0; overflow-y: auto; }

@media (max-width: 768px) {
  .theme-dialog .theme-grid { grid-template-columns: repeat(2, 1fr); }
  .theme-dialog .style-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .theme-dialog .style-row { grid-template-columns: 1fr; }
  .theme-dialog .theme-preview { height: 52px; }
}
</style>
