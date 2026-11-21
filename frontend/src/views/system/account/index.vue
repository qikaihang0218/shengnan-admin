<template>
  <div class="page-card">
    <el-row :gutter="16">
      <!-- 左侧组织树 -->
      <el-col :xs="24" :sm="24" :md="6" :lg="5" class="tree-col">
        <div class="tree-head">
          <el-input v-model="treeKeyword" placeholder="请输入部门名称" clearable size="default" :prefix-icon="Search" />
        </div>
        <div class="tree-body">
          <div class="tree-all" :class="{ active: !query.departmentId }" @click="clearDept">
            <el-icon><Files /></el-icon>
            <span>全部部门</span>
            <span v-if="!query.departmentId" class="tree-all-mark">当前</span>
          </div>
          <el-tree v-show="deptTree.length" ref="treeRef" :data="deptTree"
                   :props="{ label: 'deptName', children: 'children' }"
                   node-key="id" :expand-on-click-node="false" default-expand-all
                   :filter-node-method="filterNode" highlight-current @node-click="onDeptClick">
            <template #default="{ data }">
              <span class="tree-node">
                <el-icon class="tree-icon"><OfficeBuilding /></el-icon>
                <span>{{ data.deptName }}</span>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="!deptTree.length" description="暂无部门数据" :image-size="60" />
          <div v-else-if="treeKeyword && !treeMatched" class="tree-tip">未找到匹配的部门</div>
        </div>
      </el-col>

      <!-- 右侧列表 -->
      <el-col :xs="24" :sm="24" :md="18" :lg="19">
        <!-- 查询面板 -->
        <el-form :model="query" inline class="query-panel" @submit.prevent>
          <el-form-item label="账号">
            <el-input v-model="query.keyword" placeholder="账号 / 名称" clearable style="width:180px" @keyup.enter="search" @clear="search" />
          </el-form-item>
          <el-form-item label="部门">
            <el-tree-select v-model="query.departmentId" :data="deptTree" check-strictly clearable
                            :props="{ label: 'deptName', children: 'children' }" node-key="id"
                            placeholder="全部部门" style="width:180px" @change="onPanelDeptChange" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 工具栏 -->
        <div class="toolbar">
          <el-button v-if="store.hasPerm('account:create')" type="primary" :icon="Plus" @click="openCreate">新增</el-button>
          <el-button v-if="store.hasPerm('account:update')" type="success" :icon="Edit" :disabled="!selectedRow" @click="openEdit(selectedRow)">修改</el-button>
          <el-button v-if="store.hasPerm('account:delete')" type="danger" :icon="Delete" :disabled="!selectedRow" @click="remove(selectedRow)">删除</el-button>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <span class="toolbar-tip">
            {{ selectedRow ? `已选择：${selectedRow.displayName || selectedRow.username}` : '点击表格行可选中后修改或删除' }}
          </span>
        </div>

        <div class="table-wrap" ref="tableWrapRef">
          <el-table ref="tableRef" :data="list" v-loading="loading" border stripe size="small" highlight-current-row
                    empty-text="暂无账号数据" @current-change="r => selectedRow = r">
            <el-table-column type="index" label="#" width="50" align="center" :index="i => (query.pageNum - 1) * query.pageSize + i + 1" />
            <el-table-column prop="username" label="账号" width="95" show-overflow-tooltip />
            <el-table-column prop="displayName" label="名称" min-width="85" show-overflow-tooltip />
            <el-table-column v-if="showDept" label="部门" width="100" show-overflow-tooltip>
              <template #default="{ row }">{{ deptNameOf(row.departmentId) }}</template>
            </el-table-column>
            <el-table-column v-if="showEmail" prop="email" label="邮箱" width="140" show-overflow-tooltip />
            <el-table-column v-if="showMobile" prop="mobile" label="手机" width="110" />
            <el-table-column label="状态" width="65" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">
                  {{ row.isEnabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" :fixed="fixedRight" align="center" class-name="op-col">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top" :show-after="300">
                  <el-button v-if="store.hasPerm('account:update')" type="primary" size="small" :icon="Edit"
                             circle @click.stop="openEdit(row)" />
                </el-tooltip>
                <el-tooltip content="重置密码" placement="top" :show-after="300">
                  <el-button v-if="store.hasPerm('account:reset-pwd')" type="warning" size="small" :icon="Key"
                             circle @click.stop="resetPwd(row)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top" :show-after="300">
                  <el-button v-if="store.hasPerm('account:delete')" type="danger" size="small" :icon="Delete"
                             circle @click.stop="remove(row)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="pagination">
          <el-pagination background layout="total, sizes, prev, pager, next"
                         :total="total" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                         :page-sizes="[10, 20, 50]" @current-change="load" @size-change="load" />
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑账号' : '新增账号'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="!!editId" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="密码" v-if="!editId">
          <el-input v-model="form.password" type="password" placeholder="留空默认 123456" />
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="form.departmentId" :data="deptTree" check-strictly clearable
                          :props="{ label: 'deptName', children: 'children' }"
                          node-key="id" placeholder="选择部门" style="width:100%" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width:100%">
            <el-option v-for="r in options.roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isEnabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key, OfficeBuilding, Files } from '@element-plus/icons-vue'
import request from '../../../utils/request'
import { useTableAutoLayout } from '../../../utils/useTableAutoLayout'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), saving = ref(false)
const list = ref([]), total = ref(0), selectedRow = ref(null)
const dialogVisible = ref(false), editId = ref(null)
const formRef = ref(), treeRef = ref(), tableRef = ref(), tableWrapRef = ref()
const options = ref({ roles: [] })
const deptTree = ref([])
const treeKeyword = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', departmentId: null })
const form = reactive({ username: '', displayName: '', password: '', email: '', mobile: '', departmentId: null, roleIds: [], isEnabled: true })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const { relayout, width: tableWidth } = useTableAutoLayout(tableRef, tableWrapRef)

// 按容器宽度逐级纳入可选列，避免固定操作列压住相邻列
const BASE_WIDTH = 415 // 序号 50 + 账号 95 + 名称 85 + 状态 65 + 操作 120
const OPTIONAL_COLS = [
  { key: 'dept', width: 100 },
  { key: 'mobile', width: 100 },
  { key: 'email', width: 130 }
]
const visibleCols = computed(() => {
  const set = new Set()
  const avail = tableWidth.value
  if (!avail) return new Set(OPTIONAL_COLS.map(c => c.key))
  let used = BASE_WIDTH
  for (const col of OPTIONAL_COLS) {
    if (used + col.width > avail) break
    used += col.width
    set.add(col.key)
  }
  return set
})
const showDept = computed(() => visibleCols.value.has('dept'))
const showMobile = computed(() => visibleCols.value.has('mobile'))
const showEmail = computed(() => visibleCols.value.has('email'))
// 容器过窄时取消固定列，避免固定列压住相邻列
const fixedRight = computed(() => (tableWidth.value >= 640 ? 'right' : false))
watch(visibleCols, () => nextTick(relayout))

const flatDepts = computed(() => {
  const out = []
  const walk = nodes => nodes.forEach(n => { out.push(n); if (n.children) walk(n.children) })
  walk(deptTree.value)
  return out
})
const treeMatched = computed(() => {
  if (!treeKeyword.value) return true
  return flatDepts.value.some(d => (d.deptName || '').includes(treeKeyword.value))
})
function deptNameOf(id) {
  const d = flatDepts.value.find(x => x.id === id)
  return d ? d.deptName : '-'
}

watch(treeKeyword, v => treeRef.value?.filter(v))
function filterNode(value, data) {
  if (!value) return true
  return (data.deptName || '').includes(value)
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/account/list', { params: query })
    list.value = res.data.records
    total.value = res.data.total
    selectedRow.value = null
    relayout()
  } finally { loading.value = false }
}
async function loadTree() {
  const res = await request.get('/department/tree')
  deptTree.value = res.data || []
}
async function loadOptions() {
  const res = await request.get('/account/options')
  options.value = res.data
}
function onDeptClick(data) {
  query.departmentId = data.id
  query.pageNum = 1
  load()
}
function onPanelDeptChange(id) {
  query.departmentId = id || null
  treeRef.value?.setCurrentKey(id || null)
  query.pageNum = 1
  load()
}
function clearDept() {
  query.departmentId = null
  treeRef.value?.setCurrentKey(null)
  query.pageNum = 1
  load()
}
function search() { query.pageNum = 1; load() }
function resetQuery() {
  query.keyword = ''
  query.departmentId = null
  query.pageNum = 1
  treeKeyword.value = ''
  treeRef.value?.setCurrentKey(null)
  load()
}
function resetForm() {
  Object.assign(form, { username: '', displayName: '', password: '', email: '', mobile: '', departmentId: null, roleIds: [], isEnabled: true })
  editId.value = null
  formRef.value?.clearValidate()
}
function openCreate() {
  resetForm()
  form.departmentId = query.departmentId
  dialogVisible.value = true
}
async function openEdit(row) {
  if (!row) return
  resetForm()
  const res = await request.get('/account/' + row.id)
  Object.assign(form, res.data.account, { roleIds: res.data.roleIds, password: '' })
  editId.value = row.id
  dialogVisible.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { account: { ...form, id: editId.value || undefined }, roleIds: form.roleIds }
    if (editId.value) await request.put('/account', payload)
    else await request.post('/account', payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(row) {
  if (!row) return
  await ElMessageBox.confirm('确认删除账号 ' + row.username + '？', '提示', { type: 'warning' })
  await request.delete('/account/' + row.id)
  ElMessage.success('已删除')
  load()
}
async function resetPwd(row) {
  const { value } = await ElMessageBox.prompt('输入新密码（留空默认 123456）', '重置密码', { inputType: 'password' })
  await request.put('/account/password/' + row.id, { password: value || '123456' })
  ElMessage.success('密码已重置')
}
onMounted(() => { load(); loadTree(); loadOptions() })
</script>

<style scoped>
.tree-col {
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 16px;
}
.tree-head { margin-bottom: 10px; }
.tree-body { max-height: calc(100vh - 240px); overflow: auto; }
.tree-all {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color .2s, color .2s;
}
.tree-all:hover { background: var(--el-fill-color-light); }
.tree-all.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 600; }
.tree-all-mark { margin-left: auto; font-size: 11px; opacity: .75; }
.tree-tip { padding: 8px 10px; font-size: 12px; color: var(--el-text-color-secondary); }
.tree-node { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; }
.tree-icon { color: var(--el-color-primary); }
.toolbar-tip {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.query-panel {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px 12px 0;
  margin-bottom: 12px;
}
.query-panel :deep(.el-form-item) { margin-bottom: 12px; }
:deep(.op-col .cell) { display: flex; justify-content: center; gap: 6px; white-space: nowrap; }
:deep(.op-col .cell .el-button + .el-button) { margin-left: 0; }
@media (max-width: 768px) {
  .tree-col { border-right: none; padding-right: 0; border-bottom: 1px solid var(--el-border-color-lighter); padding-bottom: 12px; margin-bottom: 12px; }
  .tree-body { max-height: 200px; }
}
</style>