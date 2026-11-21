<template>
  <div class="page-card">
    <el-form :model="query" inline class="query-panel" @submit.prevent>
      <el-form-item label="部门名称">
        <el-input v-model="query.deptName" placeholder="请输入部门名称" clearable style="width:180px" @keyup.enter="load" @clear="load" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.isEnabled" placeholder="全部" clearable style="width:120px" @change="load">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate(null)">新增</el-button>
      <el-button v-if="canWrite" type="success" :icon="Edit" :disabled="!selectedRow" @click="openEdit(selectedRow)">修改</el-button>
      <el-button v-if="canWrite" type="warning" :icon="Plus" :disabled="!selectedRow" @click="openCreate(selectedRow)">新增下级</el-button>
      <el-button v-if="canWrite" type="danger" :icon="Delete" :disabled="!selectedRow" @click="remove(selectedRow)">删除</el-button>
      <el-button :icon="expandAll ? Fold : Expand" @click="toggleExpand">{{ expandAll ? '折叠全部' : '展开全部' }}</el-button>
      <span class="toolbar-tip">
        {{ selectedRow ? `已选择：${selectedRow.deptName}` : `共 ${totalCount} 个部门，点击行可选中后修改或删除` }}
      </span>
    </div>

    <div class="table-wrap" ref="tableWrapRef">
      <el-table ref="tableRef" :data="tableData" v-loading="loading" border stripe size="small" row-key="id"
                :tree-props="{ children: 'children' }" :default-expand-all="true"
                empty-text="暂无部门数据" highlight-current-row @current-change="r => selectedRow = r">
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column v-if="showCode" prop="deptCode" label="部门编码" width="110" show-overflow-tooltip />
        <el-table-column v-if="showLeader" prop="leaderName" label="负责人" width="90" />
        <el-table-column v-if="showContact" prop="contact" label="联系方式" width="120" />
        <el-table-column v-if="showSort" prop="sortNo" label="排序" width="65" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" :fixed="fixedRight" align="center" class-name="op-col">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top" :show-after="300">
              <el-button v-if="canWrite" type="primary" size="small" :icon="Edit" circle @click.stop="openEdit(row)" />
            </el-tooltip>
            <el-tooltip content="新增下级" placement="top" :show-after="300">
              <el-button v-if="canWrite" type="success" size="small" :icon="Plus" circle @click.stop="openCreate(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top" :show-after="300">
              <el-button v-if="canWrite" type="danger" size="small" :icon="Delete" circle @click.stop="remove(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑部门' : '新增部门'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="treeOptions" check-strictly clearable
                          :props="{ label: 'deptName', children: 'children' }"
                          node-key="id" placeholder="顶级部门" style="width:100%" />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="部门编码"><el-input v-model="form.deptCode" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leaderName" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.isEnabled" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Fold, Expand } from '@element-plus/icons-vue'
import request from '../../../utils/request'
import { useTableAutoLayout } from '../../../utils/useTableAutoLayout'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const canWrite = computed(() =>
  store.hasPerm('department:create') || store.hasPerm('department:update') ||
  store.hasPerm('department:delete') || store.hasPerm('department:write'))

const loading = ref(false), saving = ref(false)
const tree = ref([]), selectedRow = ref(null), expandAll = ref(true), tableRef = ref(), tableWrapRef = ref()
const dialogVisible = ref(false), editId = ref(null), formRef = ref()
const query = reactive({ deptName: '', isEnabled: null })
const form = reactive({ parentId: 0, deptName: '', deptCode: '', leaderName: '', contact: '', sortNo: 0, isEnabled: true })
const rules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const { relayout, width: tableWidth } = useTableAutoLayout(tableRef, tableWrapRef)

// 按容器宽度逐级纳入可选列，保证总宽不超容器，固定操作列才不会压住相邻列
const BASE_WIDTH = 400 // 部门名称 180 + 状态 90 + 操作 130
const OPTIONAL_COLS = [
  { key: 'code', width: 110 },
  { key: 'leader', width: 90 },
  { key: 'contact', width: 120 },
  { key: 'sort', width: 65 }
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
const showCode = computed(() => visibleCols.value.has('code'))
const showLeader = computed(() => visibleCols.value.has('leader'))
const showContact = computed(() => visibleCols.value.has('contact'))
const showSort = computed(() => visibleCols.value.has('sort'))
// 容器过窄时取消固定列，避免固定列压住相邻列
const fixedRight = computed(() => (tableWidth.value >= 680 ? 'right' : false))
watch(visibleCols, () => nextTick(relayout))

function filterTree(nodes) {
  const out = []
  nodes.forEach(n => {
    const hitName = !query.deptName || (n.deptName || '').includes(query.deptName)
    const hitStatus = query.isEnabled === null || query.isEnabled === '' || n.isEnabled === query.isEnabled
    const children = n.children ? filterTree(n.children) : []
    if ((hitName && hitStatus) || children.length) out.push({ ...n, children })
  })
  return out
}
const tableData = computed(() => filterTree(tree.value))
const treeOptions = computed(() => [{ id: 0, deptName: '顶级部门', children: tree.value }])
const totalCount = computed(() => {
  let n = 0
  const walk = nodes => nodes.forEach(x => { n++; if (x.children) walk(x.children) })
  walk(tree.value)
  return n
})

async function load() {
  loading.value = true
  try {
    const res = await request.get('/department/tree')
    tree.value = res.data || []
    relayout()
  } finally { loading.value = false }
}
function resetQuery() {
  query.deptName = ''
  query.isEnabled = null
  load()
}
function toggleExpand() {
  expandAll.value = !expandAll.value
  const walk = nodes => nodes.forEach(n => {
    tableRef.value?.toggleRowExpansion(n, expandAll.value)
    if (n.children) walk(n.children)
  })
  walk(tableData.value)
}
function resetForm() {
  Object.assign(form, { parentId: 0, deptName: '', deptCode: '', leaderName: '', contact: '', sortNo: 0, isEnabled: true })
  editId.value = null
  formRef.value?.clearValidate()
}
function openCreate(parent) {
  resetForm()
  form.parentId = parent ? parent.id : 0
  dialogVisible.value = true
}
function openEdit(row) {
  if (!row) return
  resetForm()
  Object.assign(form, row)
  form.parentId = row.parentId || 0
  editId.value = row.id
  dialogVisible.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, id: editId.value || undefined }
    if (editId.value) await request.put('/department', payload)
    else await request.post('/department', payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(row) {
  if (!row) return
  await ElMessageBox.confirm(`确认删除部门「${row.deptName}」？存在下级部门时请先删除下级。`, '提示', { type: 'warning' })
  await request.delete('/department/' + row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.query-panel {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px 12px 0;
  margin-bottom: 12px;
}
.query-panel :deep(.el-form-item) { margin-bottom: 12px; }
.toolbar-tip {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
:deep(.op-col .cell) { display: flex; justify-content: center; gap: 6px; white-space: nowrap; }
:deep(.op-col .cell .el-button + .el-button) { margin-left: 0; }
</style>