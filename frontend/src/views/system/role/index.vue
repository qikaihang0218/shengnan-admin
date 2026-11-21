<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('role:create')" type="success" @click="openCreate">新增角色</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column label="数据范围" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ scopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="description" label="说明" min-width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('role:grant')" link type="warning" size="small" @click="openGrant(row)">分配权限</el-button>
            <el-button v-if="store.hasPerm('role:update')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('role:delete') && row.roleCode !== 'super_admin'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑角色' : '新增角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码" prop="roleCode"><el-input v-model="form.roleCode" :disabled="!!editId" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width:100%">
            <el-option label="全部数据" value="all" />
            <el-option label="本部门" value="dept" />
            <el-option label="本部门及以下" value="dept_tree" />
            <el-option label="仅本人" value="self" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grantVisible" :title="'分配权限 · ' + (grantRole?.roleName || '')" width="520px">
      <el-tree ref="treeRef" :data="permTree" show-checkbox node-key="id"
               :default-expand-all="true" :props="{ label: 'name', children: 'children' }" />
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGrant">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), saving = ref(false)
const list = ref([]), dialogVisible = ref(false), editId = ref(null), formRef = ref()
const grantVisible = ref(false), grantRole = ref(null), treeRef = ref(), permTree = ref([])
const form = reactive({ roleName: '', roleCode: '', dataScope: 'self', sortNo: 0, description: '' })
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

function scopeLabel(s) {
  return { all: '全部数据', dept: '本部门', dept_tree: '本部门及以下', self: '仅本人' }[s] || s
}
async function load() {
  loading.value = true
  try { const res = await request.get('/role/list'); list.value = res.data || [] }
  finally { loading.value = false }
}
function resetForm() {
  Object.assign(form, { roleName: '', roleCode: '', dataScope: 'self', sortNo: 0, description: '' })
  editId.value = null; formRef.value?.clearValidate()
}
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) { resetForm(); Object.assign(form, row); editId.value = row.id; dialogVisible.value = true }
async function submit() {
  await formRef.value.validate(); saving.value = true
  try {
    const payload = { role: { ...form, id: editId.value || undefined } }
    if (editId.value) await request.put('/role', payload)
    else await request.post('/role', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } finally { saving.value = false }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除角色 ' + row.roleName + '？', '提示', { type: 'warning' })
  await request.delete('/role/' + row.id); ElMessage.success('已删除'); load()
}
async function openGrant(row) {
  grantRole.value = row; grantVisible.value = true
  const [tree, permIds] = await Promise.all([
    request.get('/permission/tree'),
    request.get('/role/permissions/' + row.id)
  ])
  permTree.value = tree.data || []
  setTimeout(() => treeRef.value?.setCheckedKeys(permIds.data || []), 0)
}
async function submitGrant() {
  const keys = treeRef.value.getCheckedKeys()
  const half = treeRef.value.getHalfCheckedKeys()
  await request.put('/role/grant/' + grantRole.value.id, { permissionIds: [...keys, ...half] })
  ElMessage.success('权限已分配'); grantVisible.value = false
}
onMounted(load)
</script>
