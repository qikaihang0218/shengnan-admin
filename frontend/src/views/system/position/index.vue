<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('position:create')" type="success" @click="openCreate">新增岗位管理</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="positionName" label="岗位名称" width="150" />
        <el-table-column prop="positionCode" label="岗位编码" width="140" />
        <el-table-column prop="sortNo" label="排序" width="90" />
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">{{ row.isEnabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('position:update') || store.hasPerm('position:write')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('position:delete') || store.hasPerm('position:write')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑岗位管理' : '新增岗位管理'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="岗位名称"><el-input v-model="form.positionName" style="width:100%" /></el-form-item>
        <el-form-item label="岗位编码"><el-input v-model="form.positionCode" style="width:100%" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" style="width:100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" style="width:100%" /></el-form-item>
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
function jsonFormDynamic() { return { positionName: '', positionCode: '', sortNo: 0, description: '' } }
const seed = jsonFormDynamic()
const form = reactive(seed)

async function load() {
  loading.value = true
  try { const res = await request.get('/position/list'); list.value = res.data || [] }
  finally { loading.value = false }
}
function resetForm() { Object.assign(form, jsonFormDynamic()); editId.value = null; formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) { resetForm(); Object.assign(form, row); editId.value = row.id; dialogVisible.value = true }
async function submit() {
  saving.value = true
  try {
    const payload = { ...form, id: editId.value || undefined }
    if (editId.value) await request.put('/position', payload)
    else await request.post('/position', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } finally { saving.value = false }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await request.delete('/position/' + row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
