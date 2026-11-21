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
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button v-if="store.hasPerm('dict:write')" link type="danger" size="small" @click.stop="removeType(row)">删除</el-button>
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
          <el-table :data="items" border stripe size="small" v-if="currentType">
            <el-table-column prop="itemLabel" label="文本" min-width="120" />
            <el-table-column prop="itemValue" label="值" min-width="100" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button v-if="store.hasPerm('dict:write')" link type="danger" size="small" @click="removeItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="请先选择左侧字典类型" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px">
      <el-form :model="form" label-width="80px">
        <template v-if="dialogKind === 'type'">
          <el-form-item label="名称"><el-input v-model="form.dictName" /></el-form-item>
          <el-form-item label="编码"><el-input v-model="form.dictCode" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="文本"><el-input v-model="form.itemLabel" /></el-form-item>
          <el-form-item label="值"><el-input v-model="form.itemValue" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
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
const loading = ref(false)
const types = ref([]), items = ref([]), currentType = ref(null)
const dialogVisible = ref(false), dialogKind = ref('type'), dialogTitle = ref('')
const form = reactive({})

async function loadTypes() {
  loading.value = true
  try { const res = await request.get('/dict/list'); types.value = res.data || [] }
  finally { loading.value = false }
}
async function selectType(row) {
  currentType.value = row
  if (row) {
    const res = await request.get('/dict-item/list')
    items.value = (res.data || []).filter(i => i.typeId === row.id)
  } else items.value = []
}
function openTypeCreate() {
  dialogKind.value = 'type'; dialogTitle.value = '新增字典类型'
  Object.assign(form, { dictName: '', dictCode: '', description: '' }); dialogVisible.value = true
}
function openItemCreate() {
  dialogKind.value = 'item'; dialogTitle.value = '新增字典项'
  Object.assign(form, { itemLabel: '', itemValue: '', sortNo: 0, typeId: currentType.value.id }); dialogVisible.value = true
}
async function submit() {
  if (dialogKind.value === 'type') {
    await request.post('/dict', { ...form }); ElMessage.success('已保存'); dialogVisible.value = false; loadTypes()
  } else {
    await request.post('/dict-item', { ...form }); ElMessage.success('已保存'); dialogVisible.value = false; selectType(currentType.value)
  }
}
async function removeType(row) {
  await ElMessageBox.confirm('删除字典类型？', '提示', { type: 'warning' })
  await request.delete('/dict/' + row.id); ElMessage.success('已删除'); loadTypes()
}
async function removeItem(row) {
  await ElMessageBox.confirm('删除字典项？', '提示', { type: 'warning' })
  await request.delete('/dict-item/' + row.id); ElMessage.success('已删除'); selectType(currentType.value)
}
onMounted(loadTypes)
</script>

<style scoped>
.card-head { display: flex; align-items: center; justify-content: space-between; }
.dict-page { height: 100%; }
</style>
