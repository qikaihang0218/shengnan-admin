<template>
  <div class="dict-page">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card class="page-card">
          <template #header>
            <div class="card-head">
              <span>字典类型</span>
              <el-button v-if="store.hasPerm('dict:write')" type="primary" size="small" @click="openTypeCreate">新增</el-button>
            </div>
          </template>
          <el-table :data="types" v-loading="loading" border stripe size="small" highlight-current-row
                    @current-change="selectType">
            <el-table-column prop="dictName" label="名称" min-width="120" />
            <el-table-column prop="dictCode" label="编码" min-width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <template v-if="store.hasPerm('dict:write')">
                  <el-button link type="primary" size="small" @click.stop="openTypeEdit(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="removeType(row)">删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card class="page-card">
          <template #header>
            <div class="card-head">
              <span>字典项 {{ currentType ? '· ' + currentType.dictName : '' }}</span>
              <el-button v-if="store.hasPerm('dict:write') && currentType" type="primary" size="small" @click="openItemCreate">新增</el-button>
            </div>
          </template>
          <el-table v-if="currentType" :data="items" border stripe size="small" v-loading="itemLoading">
            <el-table-column prop="itemLabel" label="文本" min-width="120" />
            <el-table-column prop="itemValue" label="值" min-width="100" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <template v-if="store.hasPerm('dict:write')">
                  <el-button link type="primary" size="small" @click="openItemEdit(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="removeItem(row)">删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="请先选择左侧字典类型" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <template v-if="dialogKind === 'type'">
          <el-form-item label="名称" prop="dictName"><el-input v-model="form.dictName" /></el-form-item>
          <el-form-item label="编码" prop="dictCode"><el-input v-model="form.dictCode" :disabled="!!form.id" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="文本" prop="itemLabel"><el-input v-model="form.itemLabel" /></el-form-item>
          <el-form-item label="值" prop="itemValue"><el-input v-model="form.itemValue" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
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
const loading = ref(false), itemLoading = ref(false), submitting = ref(false)
const types = ref([]), items = ref([]), currentType = ref(null)
const dialogVisible = ref(false), dialogKind = ref('type'), dialogTitle = ref('')
const formRef = ref(null)
const form = reactive({})

const rules = {
  dictName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  dictCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  itemLabel: [{ required: true, message: '请输入文本', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入值', trigger: 'blur' }]
}

async function loadTypes() {
  loading.value = true
  try {
    const res = await request.get('/dict/list')
    types.value = res.data || []
    if (currentType.value) {
      const kept = types.value.find(t => t.id === currentType.value.id)
      if (kept) currentType.value = kept
      else { currentType.value = null; items.value = [] }
    }
  } finally { loading.value = false }
}

async function loadItems() {
  if (!currentType.value) { items.value = []; return }
  itemLoading.value = true
  try {
    const res = await request.get('/dict-item/list', { params: { typeId: currentType.value.id } })
    items.value = res.data || []
  } finally { itemLoading.value = false }
}

async function selectType(row) {
  currentType.value = row || null
  await loadItems()
}

function openTypeCreate() {
  dialogKind.value = 'type'; dialogTitle.value = '新增字典类型'
  Object.assign(form, { id: null, dictName: '', dictCode: '', description: '' })
  dialogVisible.value = true
}

function openTypeEdit(row) {
  dialogKind.value = 'type'; dialogTitle.value = '编辑字典类型'
  Object.assign(form, { id: row.id, dictName: row.dictName, dictCode: row.dictCode, description: row.description || '' })
  dialogVisible.value = true
}

function openItemCreate() {
  dialogKind.value = 'item'; dialogTitle.value = '新增字典项'
  Object.assign(form, { id: null, itemLabel: '', itemValue: '', sortNo: 0, typeId: currentType.value.id })
  dialogVisible.value = true
}

function openItemEdit(row) {
  dialogKind.value = 'item'; dialogTitle.value = '编辑字典项'
  Object.assign(form, {
    id: row.id, itemLabel: row.itemLabel, itemValue: row.itemValue,
    sortNo: row.sortNo ?? 0, typeId: row.typeId
  })
  dialogVisible.value = true
}

async function submit() {
  if (formRef.value) {
    const ok = await formRef.value.validate().catch(() => false)
    if (!ok) return
  }
  submitting.value = true
  try {
    const isEdit = !!form.id
    if (dialogKind.value === 'type') {
      if (isEdit) await request.put('/dict', { ...form })
      else await request.post('/dict', { ...form })
      ElMessage.success('已保存')
      dialogVisible.value = false
      await loadTypes()
    } else {
      if (isEdit) await request.put('/dict-item', { ...form })
      else await request.post('/dict-item', { ...form })
      ElMessage.success('已保存')
      dialogVisible.value = false
      await loadItems()
    }
  } finally { submitting.value = false }
}

async function removeType(row) {
  await ElMessageBox.confirm(`删除字典类型「${row.dictName}」及其下所有字典项？`, '提示', { type: 'warning' })
  await request.delete('/dict/' + row.id)
  ElMessage.success('已删除')
  if (currentType.value && currentType.value.id === row.id) {
    currentType.value = null
    items.value = []
  }
  await loadTypes()
}

async function removeItem(row) {
  await ElMessageBox.confirm(`删除字典项「${row.itemLabel}」？`, '提示', { type: 'warning' })
  await request.delete('/dict-item/' + row.id)
  ElMessage.success('已删除')
  await loadItems()
}

onMounted(loadTypes)
</script>

<style scoped>
.card-head { display: flex; align-items: center; justify-content: space-between; }
.dict-page { height: 100%; }
</style>