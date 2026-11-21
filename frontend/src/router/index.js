import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import Layout from '../layout/index.vue'

// 静态路由
const constantRoutes = [
  { path: '/login', component: () => import('../views/login/index.vue'), meta: { public: true } },
  { path: '/', redirect: '/dashboard' }
]

// 组件映射：后端 component 字段 → 前端组件
const componentMap = {
  'Layout': Layout,
  'dashboard/index': () => import('../views/dashboard/index.vue'),
  'system/account/index': () => import('../views/system/account/index.vue'),
  'system/role/index': () => import('../views/system/role/index.vue'),
  'system/permission/index': () => import('../views/system/permission/index.vue'),
  'system/department/index': () => import('../views/system/department/index.vue'),
  'system/position/index': () => import('../views/system/position/index.vue'),
  'system/dictionary/index': () => import('../views/system/dictionary/index.vue'),
  'system/config/index': () => import('../views/system/config/index.vue'),
  'system/notice/index': () => import('../views/system/notice/index.vue'),
  'monitor/login-log/index': () => import('../views/monitor/login-log/index.vue'),
  'monitor/operation-log/index': () => import('../views/monitor/operation-log/index.vue'),
  'monitor/job/index': () => import('../views/monitor/job/index.vue'),
  'monitor/file/index': () => import('../views/monitor/file/index.vue'),
  'repair/order/index': () => import('../views/repair/order/index.vue')
}

// 把后端菜单树转换成 Vue Router 路由：所有顶级菜单统一套 Layout 外壳
function buildRoutes(nodes) {
  const result = []
  for (const node of nodes) {
    if (node.type === 'action' || node.visible === false) continue
    const fullPath = node.path ? (node.path.startsWith('/') ? node.path : '/' + node.path) : '/'
    const meta = { title: node.name, icon: node.icon, id: node.id }
    const children = (node.children || []).filter(c => c.type !== 'action' && c.visible !== false)

    if (children.length) {
      // 目录节点：Layout 外壳 + 子页面路由
      result.push({
        path: fullPath,
        name: 'route_' + node.id,
        component: Layout,
        meta,
        children: children.map(child => ({
          path: child.path.startsWith('/') ? child.path.slice(1) : child.path,
          name: 'route_' + child.id,
          component: componentMap[child.component] || componentMap['dashboard/index'],
          meta: { title: child.name, icon: child.icon, id: child.id }
        }))
      })
    } else {
      // 顶级单页（如工作台）：Layout 外壳 + 空路径子路由承载页面内容
      result.push({
        path: fullPath,
        name: 'route_' + node.id,
        component: Layout,
        meta,
        children: [{
          path: '',
          name: 'route_' + node.id + '_content',
          component: componentMap[node.component] || componentMap['dashboard/index'],
          meta
        }]
      })
    }
  }
  return result
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

// 动态注入后端返回的路由（只注入一次）
let routesLoaded = false
export function injectRoutes(nodes) {
  if (routesLoaded) return
  const dynamic = buildRoutes(nodes)
  dynamic.forEach(r => router.addRoute(r))
  router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/dashboard' })
  routesLoaded = true
}

router.beforeEach(async (to, from) => {
  const store = useUserStore()
  if (to.meta.public) return true
  // 尚未探测登录态（首次进入或刷新）：靠 HttpOnly Cookie 自动恢复会话
  if (!store.checked) {
    try {
      await store.loadUserInfo()
    } catch (e) {
      store.logout()
      return { path: '/login' }
    }
  }
  if (!routesLoaded) {
    const nodes = await store.loadRoutes()
    injectRoutes(nodes)
    // 路由刚注入，需按原目标重新解析一次；根路径则落到第一个菜单
    if (to.path === '/') return { path: nodes[0]?.path || '/dashboard', replace: true }
    return { path: to.fullPath, replace: true }
  }
  return true
})

export default router
