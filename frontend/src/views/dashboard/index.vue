<template>
  <div>
    <div class="stat-grid">
      <el-card class="stat-card" shadow="hover" v-for="card in cards" :key="card.title">
        <div class="stat-inner">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="24"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
        </div>
      </el-card>
    </div>
    <el-card class="welcome-card" style="margin-top:16px">
      <template #header><span>欢迎使用胜楠管理平台</span></template>
      <p style="color:#606266;line-height:1.8">
        这是一个独立实现的企业级权限管理系统。当前登录账号为
        <strong>{{ store.username }}</strong>，数据范围为
        <el-tag size="small">{{ scopeLabel }}</el-tag>。
      </p>
      <p style="color:#909399;line-height:1.8;margin-top:8px">
        左侧菜单由后端根据您的权限动态生成。可在「系统管理」中维护账号、角色、权限资源，
        在「系统监控」中查看日志、管理文件与定时任务。
      </p>
    </el-card>

    <el-card class="welcome-card" style="margin-top:16px">
      <template #header>
        <span>通知公告</span>
        <el-button link type="primary" size="small" style="float:right" @click="loadNotices">刷新</el-button>
      </template>
      <div v-if="notices.length === 0" class="text-muted">暂无通知公告</div>
      <div v-for="n in notices" :key="n.id" class="notice-row">
        <el-tag :type="n.noticeType === 2 ? 'success' : 'info'" size="small">{{ n.typeName || (n.noticeType === 2 ? '公告' : '通知') }}</el-tag>
        <el-tag v-if="n.isTop" type="warning" size="small">置顶</el-tag>
        <span class="notice-row-title" @click="toggleNotice(n.id)">{{ n.title }}</span>
        <span class="text-muted notice-row-time">{{ n.publishTime || n.createdAt }}</span>
      </div>
      <div v-for="n in notices" :key="'c-' + n.id" v-show="expandedNotice === n.id" class="notice-row-body">
        <SafeMarkdown :content="n.content" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../../utils/request'
import { useUserStore } from '../../store/user'
import SafeMarkdown from '../../components/SafeMarkdown.vue'

const store = useUserStore()
const counts = ref({ accounts: 0, roles: 0, permissions: 0, files: 0 })
const notices = ref([])
const expandedNotice = ref(null)
function toggleNotice(id) { expandedNotice.value = expandedNotice.value === id ? null : id }
const scopeLabel = computed(() => ({
  all: '全部数据', dept: '本部门', dept_tree: '本部门及以下', self: '仅本人'
}[store.dataScope] || '仅本人'))

const cards = computed(() => [
  { title: '账号数', value: counts.value.accounts, icon: 'User', color: '#409eff' },
  { title: '角色数', value: counts.value.roles, icon: 'UserFilled', color: '#67c23a' },
  { title: '权限资源', value: counts.value.permissions, icon: 'Grid', color: '#e6a23c' },
  { title: '文件数', value: counts.value.files, icon: 'FolderOpened', color: '#909399' }
])

async function loadCounts() {
  try {
    const [a, r, p, f] = await Promise.all([
      request.get('/account/list', { params: { pageNum: 1, pageSize: 1 } }),
      request.get('/role/list'),
      request.get('/permission/tree'),
      request.get('/file/list')
    ])
    counts.value = {
      accounts: a.data.total || 0,
      roles: (r.data || []).length,
      permissions: (p.data || []).length,
      files: (f.data || []).length
    }
  } catch (e) { /* 统计加载失败不影响页面 */ }
}
async function loadNotices() {
  try { const res = await request.get('/notice/published'); notices.value = (res.data || []).slice(0, 5) }
  catch (e) { /* ignore */ }
}
onMounted(() => { loadCounts(); loadNotices() })
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  width: 100%;
}
@media (max-width: 1200px) { .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 700px) { .stat-grid { grid-template-columns: minmax(0, 1fr); } }
.stat-card { border-radius: 8px; min-width: 0; }
.stat-inner { display: flex; align-items: center; gap: 12px; min-width: 0; }
.stat-icon {
  width: 56px; height: 56px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.stat-value { font-size: 26px; font-weight: 600; }
.stat-title { color: #909399; font-size: 13px; margin-top: 2px; }
.welcome-card { border-radius: 8px; }
.notice-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.notice-row:last-child { border-bottom: none; }
.notice-row-title { color: #303133; cursor: pointer; }
.notice-row-title:hover { color: var(--el-color-primary, #409eff); }
.notice-row-time { margin-left: auto; }
.notice-row-body { padding: 4px 12px 12px; margin-bottom: 8px; border-radius: 6px;
  background: var(--el-fill-color-light, #f5f7fa); font-size: 13px; }
</style>
