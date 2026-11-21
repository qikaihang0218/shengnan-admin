<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('notice:create')" type="primary" @click="openCreate">发布通知</el-button>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="reload">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
      </el-select>
      <el-input v-model="query.title" placeholder="搜索标题" clearable style="width:200px" @keyup.enter="reload" />
      <el-button @click="reload">查询</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="置顶" width="70" align="center">
          <template #default="{ row }"><el-tag v-if="row.isTop" type="warning" size="small">置顶</el-tag></template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.noticeType === 2 ? 'success' : 'info'" size="small">{{ row.noticeType === 2 ? '公告' : '通知' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column prop="createdBy" label="创建人" width="110" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('notice:update')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('notice:publish')" link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '撤回' : '发布' }}
            </el-button>
            <el-button v-if="store.hasPerm('notice:delete')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="pageNum" :page-size="10" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑通知' : '发布通知'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" placeholder="请输入通知标题" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.noticeType">
            <el-radio :value="1">通知</el-radio>
            <el-radio :value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <div style="width:100%">
            <div class="editor-toolbar">
              <el-radio-group v-model="editTab" size="small">
                <el-radio-button value="edit">编辑</el-radio-button>
                <el-radio-button value="preview">预览</el-radio-button>
              </el-radio-group>
              <span class="md-hint">支持 Markdown：# 标题、**粗体**、*斜体*、- 列表、&gt; 引用、`代码`、```代码块```、[链接](https://...)</span>
            </div>
            <el-input v-show="editTab === 'edit'" v-model="form.content" type="textarea" :rows="10"
                      placeholder="请输入通知内容（支持 Markdown 语法，HTML 会被安全过滤）" />
            <div v-show="editTab === 'preview'" class="md-preview">
              <SafeMarkdown v-if="form.content" :content="form.content" />
              <span v-else class="text-muted">暂无内容</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="保存为">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">草稿</el-radio>
            <el-radio :value="1">直接发布</el-radio>
          </el-radio-group>
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
import SafeMarkdown from '../../../components/SafeMarkdown.vue'

const store = useUserStore()
const loading = ref(false), saving = ref(false)
const editTab = ref('edit')
const list = ref([]), total = ref(0), pageNum = ref(1)
const query = reactive({ status: null, title: '' })
const dialogVisible = ref(false), editId = ref(null), formRef = ref()
const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }
function emptyForm() { return { title: '', content: '', noticeType: 1, isTop: false, status: 0 } }
const form = reactive(emptyForm())

async function load() {
  loading.value = true
  try {
    const res = await request.get('/notice/list', {
      params: { pageNum: pageNum.value, pageSize: 10, status: query.status, title: query.title }
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function reload() { pageNum.value = 1; load() }
function resetForm() { Object.assign(form, emptyForm()); editId.value = null; formRef.value?.clearValidate() }
function openCreate() { resetForm(); editTab.value = 'edit'; dialogVisible.value = true }
function openEdit(row) {
  resetForm()
  Object.assign(form, { title: row.title, content: row.content, noticeType: row.noticeType, isTop: row.isTop, status: row.status })
  editId.value = row.id
  editTab.value = 'edit'
  dialogVisible.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, id: editId.value || undefined }
    if (editId.value) await request.put('/notice', payload)
    else await request.post('/notice', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } finally { saving.value = false }
}
async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(target === 1 ? '确认发布？发布后所有账号可见' : '确认撤回？撤回后不再展示', '提示', { type: 'warning' })
  await request.put('/notice/status/' + row.id, { status: target })
  ElMessage.success(target === 1 ? '已发布' : '已撤回'); load()
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除该通知？', '提示', { type: 'warning' })
  await request.delete('/notice/' + row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

<style scoped>
.editor-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.md-hint { font-size: 12px; color: var(--el-text-color-secondary, #909399); }
.md-preview { min-height: 200px; padding: 10px 12px; border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 4px; font-size: 14px; overflow-wrap: anywhere; }
</style>
