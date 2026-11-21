<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('permission:create')" type="success" @click="openCreate">新增资源</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="treeData" v-loading="loading" border size="small"
                row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="资源名称" min-width="180" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" width="140" />
        <el-table-column prop="permission" label="权限标识" min-width="160">
          <template #default="{ row }"><code v-if="row.permission">{{ row.permission }}</code><span v-else>-</span></template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="70" align="center" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('permission:update')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('permission:delete')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑资源' : '新增资源'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="父级资源">
          <el-tree-select v-model="form.parentId" :data="parentOptions" node-key="id"
                          :props="{ label: 'name', children: 'children' }" check-strictly
                          placeholder="根目录" clearable style="width:100%" />
        </el-form-item>
        <el-form-item label="资源名称"><el-input v-model="form.resourceName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.resourceType" style="width:100%">
            <el-option label="目录 catalog" value="catalog" />
            <el-option label="菜单 menu" value="menu" />
            <el-option label="动作 action" value="action" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由地址"><el-input v-model="form.routePath" placeholder="如 system/account" /></el-form-item>
        <el-form-item label="组件路径"><el-input v-model="form.component" placeholder="如 system/account/index" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.iconName" placeholder="如 User" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permission" placeholder="如 account:create" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="可见">
          <el-switch v-model="form.isVisible" active-text="是" inactive-text="否" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), treeData = ref([]), dialogVisible = ref(false), editId = ref(null)
const form = reactive({ parentId: 0, resourceName: '', resourceType: 'menu', routePath: '', component: '', iconName: '', permission: '', sortNo: 0, isVisible: true })
const parentOptions = computed(() => [{ id: 0, name: '根目录', children: treeData.value }])

function typeLabel(t) { return { catalog: '目录', menu: '菜单', action: '动作' }[t] || t }
function typeTag(t) { return { catalog: '', menu: 'success', action: 'warning' }[t] || '' }

async function load() {
  loading.value = true
  try { const res = await request.get('/permission/tree'); treeData.value = res.data || [] }
  finally { loading.value = false }
}
function resetForm() {
  Object.assign(form, { parentId: 0, resourceName: '', resourceType: 'menu', routePath: '', component: '', iconName: '', permission: '', sortNo: 0, isVisible: true })
  editId.value = null
}
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) {
  resetForm()
  Object.assign(form, { id: row.id, parentId: row.parentId, resourceName: row.name, resourceType: row.type,
    routePath: row.path, component: row.component, iconName: row.icon, permission: row.permission,
    sortNo: row.sortNo, isVisible: row.visible })
  editId.value = row.id; dialogVisible.value = true
}
async function submit() {
  const payload = { ...form, id: editId.value || undefined }
  if (editId.value) await request.put('/permission', payload)
  else await request.post('/permission', payload)
  ElMessage.success('保存成功'); dialogVisible.value = false; load()
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除资源 ' + row.name + '？', '提示', { type: 'warning' })
  await request.delete('/permission/' + row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
