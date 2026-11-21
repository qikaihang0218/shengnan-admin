<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('config:create')" type="success" @click="openCreate">新增系统参数</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configName" label="参数名称" width="150" />
        <el-table-column prop="configKey" label="参数键" width="180" />
        <el-table-column prop="configValue" label="参数值" width="180" />
        <el-table-column label="内置" width="80" align="center"><template #default="{ row }"><el-tag :type="row.isBuiltin ? 'warning' : 'info'" size="small">{{ row.isBuiltin ? '是' : '否' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('config:update') || store.hasPerm('config:write')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('config:delete') || store.hasPerm('config:write')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑系统参数' : '新增系统参数'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="参数名称"><el-input v-model="form.configName" style="width:100%" /></el-form-item>
        <el-form-item label="参数键"><el-input v-model="form.configKey" style="width:100%" /></el-form-item>
        <el-form-item label="参数值"><el-input v-model="form.configValue" style="width:100%" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" style="width:100%" /></el-form-item>
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
function jsonFormDynamic() { return { configName: '', configKey: '', configValue: '', description: '' } }
const seed = jsonFormDynamic()
const form = reactive(seed)

async function load() {
  loading.value = true
  try { const res = await request.get('/config/list'); list.value = res.data || [] }
  finally { loading.value = false }
}
function resetForm() { Object.assign(form, jsonFormDynamic()); editId.value = null; formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) { resetForm(); Object.assign(form, row); editId.value = row.id; dialogVisible.value = true }
async function submit() {
  saving.value = true
  try {
    const payload = { ...form, id: editId.value || undefined }
    if (editId.value) await request.put('/config', payload)
    else await request.post('/config', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } finally { saving.value = false }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await request.delete('/config/' + row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
