<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="账号/名称" clearable style="width:200px"
                @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="store.hasPerm('account:create')" type="success" @click="openCreate">新增账号</el-button>
    </div>

    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="displayName" label="名称" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="mobile" label="手机" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">
              {{ row.isEnabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('account:update')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('account:reset-pwd')" link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
            <el-button v-if="store.hasPerm('account:delete')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination background layout="total, sizes, prev, pager, next"
                     :total="total" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                     :page-sizes="[10, 20, 50]" @current-change="load" @size-change="load" />
    </div>

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
          <el-select v-model="form.departmentId" clearable placeholder="选择部门" style="width:100%">
            <el-option v-for="d in options.departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), saving = ref(false)
const list = ref([]), total = ref(0)
const dialogVisible = ref(false), editId = ref(null)
const formRef = ref()
const options = ref({ roles: [], departments: [] })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const form = reactive({ username: '', displayName: '', password: '', email: '', mobile: '', departmentId: null, roleIds: [], isEnabled: true })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/account/list', { params: query })
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
async function loadOptions() {
  const res = await request.get('/account/options')
  options.value = res.data
}
function resetForm() {
  Object.assign(form, { username: '', displayName: '', password: '', email: '', mobile: '', departmentId: null, roleIds: [], isEnabled: true })
  editId.value = null
  formRef.value?.clearValidate()
}
function openCreate() { resetForm(); dialogVisible.value = true }
async function openEdit(row) {
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
onMounted(() => { load(); loadOptions() })
</script>
