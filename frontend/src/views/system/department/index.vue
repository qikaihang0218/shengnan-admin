<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('department:create')" type="success" @click="openCreate">新增部门管理</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="deptName" label="部门名称" width="150" />
        <el-table-column prop="deptCode" label="部门编码" width="130" />
        <el-table-column prop="leaderName" label="负责人" width="120" />
        <el-table-column prop="contact" label="联系方式" width="140" />
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('department:update') || store.hasPerm('department:write')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('department:delete') || store.hasPerm('department:write')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑部门管理' : '新增部门管理'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="部门名称"><el-input v-model="form.deptName" style="width:100%" /></el-form-item>
        <el-form-item label="部门编码"><el-input v-model="form.deptCode" style="width:100%" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leaderName" style="width:100%" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contact" style="width:100%" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
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
function jsonFormDynamic() { return { deptName: '', deptCode: '', leaderName: '', contact: '', sortNo: 0 } }
const seed = jsonFormDynamic()
const form = reactive(seed)

async function load() {
  loading.value = true
  try { const res = await request.get('/department/list'); list.value = res.data || [] }
  finally { loading.value = false }
}
function resetForm() { Object.assign(form, jsonFormDynamic()); editId.value = null; formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) { resetForm(); Object.assign(form, row); editId.value = row.id; dialogVisible.value = true }
async function submit() {
  saving.value = true
  try {
    const payload = { ...form, id: editId.value || undefined }
    if (editId.value) await request.put('/department', payload)
    else await request.post('/department', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } finally { saving.value = false }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await request.delete('/department/' + row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
