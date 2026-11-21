import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'

export const useUserStore = defineStore('user', () => {
  // 认证状态由后端 HttpOnly Cookie 承载，前端不接触令牌本身。
  // checked 表示本次会话是否已探测过登录态（刷新后重置为 false，靠 Cookie 自动恢复）。
  const checked = ref(false)
  const username = ref('')
  const permissions = ref([])
  const roles = ref([])
  const dataScope = ref('self')
  const routes = ref([])

  async function login(usernameInput, password, captchaId, captchaCode) {
    const res = await request.post('/auth/login', {
      username: usernameInput, password, captchaId, captchaCode
    })
    applyInfo(res.data)
    checked.value = true
    await loadRoutes()
  }

  function applyInfo(data) {
    username.value = data.username || ''
    permissions.value = data.permissions || []
    roles.value = data.roles || []
    dataScope.value = data.dataScope || 'self'
  }

  async function loadUserInfo() {
    const res = await request.get('/auth/info')
    applyInfo(res.data)
    checked.value = true
  }

  async function loadRoutes() {
    const res = await request.get('/permission/routes')
    routes.value = res.data || []
    return routes.value
  }

  function hasPerm(perm) {
    if (roles.value.includes('super_admin')) return true
    return permissions.value.includes(perm)
  }

  function logout() {
    // 通知后端拉黑令牌并清除 HttpOnly Cookie；前端只清理内存状态
    request.post('/auth/logout').catch(() => {})
    checked.value = false
    username.value = ''
    permissions.value = []
    roles.value = []
    routes.value = []
  }

  return { checked, username, permissions, roles, dataScope, routes,
           login, applyInfo, loadUserInfo, loadRoutes, hasPerm, logout }
})
