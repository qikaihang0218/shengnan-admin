<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo" @click="collapsed = !collapsed">
        <span class="logo-mark"><LeafLogo /></span>
        <span v-show="!collapsed" class="logo-text">胜楠管理平台</span>
      </div>
      <el-scrollbar>
        <el-menu :default-active="route.path" :collapse="collapsed" router
                 background-color="var(--sn-side-bg)" text-color="var(--sn-side-text)" active-text-color="#fff">
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
          <el-icon class="fold-btn" @click="collapsed = !collapsed">
            <component :is="collapsed ? 'Expand' : 'Fold'" />
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
          <el-dropdown @command="onThemeChange">
            <span class="theme-trigger">
              <el-icon><Brush /></el-icon>
              {{ themeLabel }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="t in themes" :key="t.key" :command="t.key"
                                  :disabled="t.key === themeStore.current">
                  <span class="theme-dot" :style="{ background: t.primary }"></span>
                  {{ t.name }}
                  <el-icon v-if="t.key === themeStore.current" style="margin-left:8px"><Check /></el-icon>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

  <!-- 全员通知抽屉 -->
  <el-drawer v-model="noticeVisible" title="通知公告" size="420px">
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { useThemeStore, THEMES } from '../store/theme'
import request from '../utils/request'
import LeafLogo from '../components/LeafLogo.vue'
import SafeMarkdown from '../components/SafeMarkdown.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const themeStore = useThemeStore()
const themes = THEMES
const themeLabel = computed(() => THEMES.find(t => t.key === themeStore.current)?.name || '主题')
function onThemeChange(key) { themeStore.apply(key) }
const collapsed = ref(false)
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
.layout { height: 100vh; }
.aside { background: var(--sn-side-bg); overflow: hidden; transition: width .2s; }
.content-container { min-width: 0; overflow: hidden; background: var(--el-bg-color-page); }
.logo {
  height: 60px; display: flex; align-items: center; gap: 10px;
  padding: 0 16px; cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,.1);
  color: var(--sn-side-active); overflow: hidden; white-space: nowrap;
}
.logo-mark {
  width: 32px; height: 32px; padding: 6px;
  background: #fff; color: var(--sn-logo-color); border-radius: 8px; font-weight: bold; flex-shrink: 0;
}
.logo-text { font-size: 16px; font-weight: 600; }
.aside :deep(.el-menu) { border-right: none; }
.header {
  height: 60px; background: var(--el-bg-color); display: flex;
  align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
}
.header-left, .header-right { display: flex; align-items: center; gap: 16px; }
.theme-trigger { display: flex; align-items: center; gap: 6px; cursor: pointer; color: var(--el-text-color-regular); }
.theme-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.fold-btn { font-size: 20px; cursor: pointer; }
.user-trigger { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.main { padding: 20px; overflow-y: auto; overflow-x: hidden; }
.notice-bell { cursor: pointer; color: #606266; }
.notice-toolbar { display: flex; justify-content: flex-end; gap: 12px; margin-bottom: 12px; }
.notice-empty { color: #909399; text-align: center; padding: 40px 0; }
.notice-item {
  padding: 12px 14px; border: 1px solid #ebeef5; border-radius: 8px;
  margin-bottom: 10px; cursor: pointer; background: #fff;
}
.notice-item.unread { background: #f0f7ff; border-color: #c6e2ff; }
.notice-item-head { display: flex; align-items: center; gap: 6px; }
.notice-time { margin-left: auto; color: #909399; font-size: 12px; }
.notice-dot { width: 8px; height: 8px; border-radius: 50%; background: #f56c6c; }
.notice-title { font-weight: 600; margin: 8px 0 4px; color: #303133; }
.notice-content { color: #606266; font-size: 13px; line-height: 1.6; white-space: pre-wrap; }
</style>
