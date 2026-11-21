<template>
  <div class="page-card">
    <div class="stat-row">
      <div v-for="s in statCards" :key="s.key" class="stat-card" :class="'is-' + s.key" @click="quickFilter(s.key)">
        <div class="stat-num">{{ stats[s.key] ?? 0 }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <div class="toolbar">
      <el-button v-if="store.hasPerm('repair:create')" type="primary" @click="openCreate">提交报修</el-button>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="reload">
        <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="query.priority" placeholder="优先级" clearable style="width:110px" @change="reload">
        <el-option v-for="p in PRIORITY" :key="p.value" :label="p.label" :value="p.value" />
      </el-select>
      <el-select v-model="query.category" placeholder="类别" clearable style="width:130px" @change="reload">
        <el-option v-for="c in categories" :key="c.itemValue" :label="c.itemLabel" :value="c.itemValue" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="工单号/标题/地点" clearable style="width:200px" @keyup.enter="reload" />
      <el-button @click="reload">查询</el-button>
      <el-button @click="load">刷新</el-button>
      <el-checkbox v-model="query.onlyMine" @change="reload">只看我的</el-checkbox>
    </div>

    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="orderNo" label="工单号" width="140" show-overflow-tooltip />
        <el-table-column prop="title" label="报修标题" min-width="140" show-overflow-tooltip />
        <el-table-column label="类别" width="85" align="center">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="报修地点" min-width="110" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="报修人" width="75" />
        <el-table-column label="处理人" width="75">
          <template #default="{ row }">{{ row.handlerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-dropdown trigger="click" style="margin-left:8px">
              <el-button link type="primary" size="small">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canAssign(row)" @click="openAssign(row)">派单</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 1 && store.hasPerm('repair:finish')" @click="doStart(row)">开始处理</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 2 && store.hasPerm('repair:finish')" @click="openFinish(row)">完工</el-dropdown-item>
                  <el-dropdown-item v-if="canEdit(row)" @click="openEdit(row)">编辑</el-dropdown-item>
                  <el-dropdown-item v-if="canClose(row)" @click="doClose(row)">关闭</el-dropdown-item>
                  <el-dropdown-item v-if="store.hasPerm('repair:delete')" divided @click="remove(row)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="pageNum" :page-size="10" @current-change="load" />
    </div>

    <!-- 提交/编辑 -->
    <el-dialog v-model="formVisible" :title="editId ? '编辑工单' : '提交报修'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="报修标题" prop="title">
          <el-input v-model="form.title" placeholder="简要描述故障现象" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="报修类别">
          <el-select v-model="form.category" placeholder="请选择类别" style="width:100%">
            <el-option v-for="c in categories" :key="c.itemValue" :label="c.itemLabel" :value="c.itemValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio v-for="p in PRIORITY" :key="p.value" :value="p.value">{{ p.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报修地点" prop="location">
          <el-input v-model="form.location" placeholder="如：总部三楼 301 会议室" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述故障情况" />
        </el-form-item>
        <el-form-item label="故障照片">
          <el-upload v-model:file-list="fileList" list-type="picture-card" accept="image/*" multiple
                     :http-request="uploadImage" :limit="9" :on-remove="syncImages" :on-exceed="onExceed">
            <span class="upload-plus">+</span>
          </el-upload>
          <div class="text-muted tip">可一次选择多张（jpg/png/gif），最多 9 张，单张不超过 10MB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 派单 -->
    <el-dialog v-model="assignVisible" title="派单" width="460px">
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="处理人" required>
          <el-select v-model="assignForm.handlerId" placeholder="请选择处理人" filterable style="width:100%">
            <el-option v-for="h in handlers" :key="h.id"
                       :label="(h.displayName || h.username) + ' (' + h.username + ')'" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望完成">
          <el-date-picker v-model="assignForm.expectTime" type="datetime" placeholder="选填"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 完工 -->
    <el-dialog v-model="finishVisible" title="填写处理结果" width="480px">
      <el-form label-width="90px">
        <el-form-item label="处理结果" required>
          <el-input v-model="finishDesc" type="textarea" :rows="4" placeholder="请填写处理过程与结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitFinish">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="工单详情" size="min(560px, 92vw)">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="工单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ categoryLabel(detail.category) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="priorityTag(detail.priority)" size="small">{{ priorityLabel(detail.priority) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报修人">{{ detail.applicantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地点" :span="2">{{ detail.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ detail.handlerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ fmt(detail.expectTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmt(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ fmt(detail.finishTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="故障照片" :span="2">
            <div v-if="detailImages.length" class="img-grid">
              <el-image v-for="(u, i) in detailImages" :key="i" :src="u" :preview-src-list="detailImages"
                        :initial-index="i" fit="cover" class="img-thumb" preview-teleported />
            </div>
            <span v-else class="text-muted">无</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2">{{ detail.resultDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="满意度" :span="2">
            <el-rate v-if="detail.rating" :model-value="detail.rating" disabled />
            <span v-else class="text-muted">未评价</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.status === 3 && !detail.rating && isApplicant(detail)" class="rate-box">
          <span>请评价本次服务：</span>
          <el-rate v-model="rateValue" />
          <el-button type="primary" size="small" @click="doRate">提交评价</el-button>
        </div>

        <div class="log-title">流转记录</div>
        <el-timeline>
          <el-timeline-item v-for="l in (detail.logs || [])" :key="l.id"
                            :timestamp="fmt(l.createdAt)" placement="top">
            <b>{{ l.action }}</b>
            <span class="text-muted"> · {{ l.operator }}</span>
            <div class="log-content">{{ l.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), saving = ref(false)
const list = ref([]), total = ref(0), pageNum = ref(1)
const stats = reactive({})
const categories = ref([]), handlers = ref([])
const query = reactive({ status: null, priority: null, category: '', keyword: '', onlyMine: false })

const STATUS = [
  { value: 0, label: '待受理' }, { value: 1, label: '已派单' },
  { value: 2, label: '处理中' }, { value: 3, label: '已完成' }, { value: 4, label: '已关闭' }
]
const PRIORITY = [
  { value: 0, label: '低' }, { value: 1, label: '中' }, { value: 2, label: '高' }, { value: 3, label: '紧急' }
]
const statCards = [
  { key: 'total', label: '全部工单' }, { key: 'pending', label: '待受理' },
  { key: 'assigned', label: '已派单' }, { key: 'processing', label: '处理中' },
  { key: 'finished', label: '已完成' }, { key: 'closed', label: '已关闭' }
]

function statusLabel(v) { return STATUS.find(s => s.value === v)?.label || '-' }
function statusTag(v) { return ['info', 'warning', 'primary', 'success', 'info'][v] || 'info' }
function priorityLabel(v) { return PRIORITY.find(p => p.value === v)?.label || '-' }
function priorityTag(v) { return ['info', '', 'warning', 'danger'][v] || 'info' }
function categoryLabel(v) { return categories.value.find(c => c.itemValue === v)?.itemLabel || v || '-' }
function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '' }

const formVisible = ref(false), editId = ref(null), formRef = ref()
const rules = {
  title: [{ required: true, message: '请输入报修标题', trigger: 'blur' }],
  location: [{ required: true, message: '请输入报修地点', trigger: 'blur' }]
}
function emptyForm() {
  return { title: '', category: '', priority: 1, location: '', contactName: '', contactPhone: '', description: '', images: '' }
}
const form = reactive(emptyForm())

/* ---- 故障照片上传 ---- */
const fileList = ref([])
const idMap = ref({})
function imageUrl(id) { return '/api/repair/image/' + id }
function syncImages() {
  form.images = collectImages()
}
function collectImages() {
  return fileList.value
    .map(f => idMap.value[f.uid] || (f.response && f.response.data ? f.response.data.id : null))
    .filter(v => v != null && /^\d+$/.test(String(v)))
    .join(',')
}
async function uploadImage(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  try {
    const res = await request.post('/repair/image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    idMap.value[option.file.uid] = res.data.id
    option.onSuccess(res)
    syncImages()
  } catch (e) {
    option.onError(e)
    ElMessage.error(`${option.file.name} 上传失败`)
  }
}
function onExceed() {
  ElMessage.warning('最多上传 9 张照片')
}
const detailImages = computed(() => {
  const raw = detail.value && detail.value.images
  if (!raw) return []
  return raw.split(',').filter(Boolean).map(imageUrl)
})

const assignVisible = ref(false), assignForm = reactive({ id: null, handlerId: null, expectTime: '' })
const finishVisible = ref(false), finishDesc = ref(''), finishId = ref(null)
const detailVisible = ref(false), detail = ref(null), rateValue = ref(5)

async function loadStats() {
  const res = await request.get('/repair/stats')
  Object.assign(stats, res.data || {})
}
async function load() {
  loading.value = true
  try {
    const res = await request.get('/repair/list', {
      params: { pageNum: pageNum.value, pageSize: 10, ...query }
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
async function reload() { pageNum.value = 1; load(); loadStats() }
function quickFilter(key) {
  const map = { total: null, pending: 0, assigned: 1, processing: 2, finished: 3, closed: 4 }
  query.status = map[key] ?? null
  reload()
}
async function loadDict() {
  try { const r = await request.get('/repair/categories'); categories.value = r.data || [] }
  catch (e) { categories.value = [] }
}
async function loadHandlers() {
  try { const r = await request.get('/repair/handlers'); handlers.value = r.data || [] }
  catch (e) { handlers.value = [] }
}

function isApplicant(row) { return row.applicantName === store.username || store.roles.includes('super_admin') }
function canAssign(row) { return store.hasPerm('repair:assign') && row.status !== null && row.status < 3 }
function canEdit(row) { return store.hasPerm('repair:update') && row.status !== null && row.status < 3 }
function canClose(row) { return store.hasPerm('repair:finish') && row.status !== 4 }

function resetForm() {
  Object.assign(form, emptyForm())
  fileList.value = []
  idMap.value = {}
  editId.value = null
  formRef.value?.clearValidate()
}
function openCreate() { resetForm(); formVisible.value = true }
function openEdit(row) {
  resetForm()
  Object.assign(form, {
    title: row.title, category: row.category, priority: row.priority, location: row.location,
    contactName: row.contactName, contactPhone: row.contactPhone, description: row.description,
    images: row.images || ''
  })
  fileList.value = (row.images || '').split(',').filter(Boolean)
    .map(id => ({ name: 'image-' + id, url: imageUrl(id), uid: Number(id) }))
  idMap.value = {}
  fileList.value.forEach(f => { idMap.value[f.uid] = f.uid })
  editId.value = row.id
  formVisible.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, images: collectImages(), id: editId.value || undefined }
    if (editId.value) await request.put('/repair', payload)
    else await request.post('/repair', payload)
    ElMessage.success('保存成功'); formVisible.value = false; reload()
  } finally { saving.value = false }
}

function openAssign(row) {
  assignForm.id = row.id; assignForm.handlerId = row.handlerId || null; assignForm.expectTime = ''
  assignVisible.value = true
}
async function submitAssign() {
  if (!assignForm.handlerId) { ElMessage.warning('请选择处理人'); return }
  saving.value = true
  try {
    await request.put('/repair/assign/' + assignForm.id, {
      handlerId: assignForm.handlerId, expectTime: assignForm.expectTime || null
    })
    ElMessage.success('派单成功'); assignVisible.value = false; reload()
  } finally { saving.value = false }
}

async function doStart(row) {
  await ElMessageBox.confirm('确认开始处理该工单？', '提示', { type: 'warning' })
  await request.put('/repair/start/' + row.id)
  ElMessage.success('已开始处理'); reload()
}
function openFinish(row) { finishId.value = row.id; finishDesc.value = ''; finishVisible.value = true }
async function submitFinish() {
  if (!finishDesc.value.trim()) { ElMessage.warning('请填写处理结果'); return }
  saving.value = true
  try {
    await request.put('/repair/finish/' + finishId.value, { resultDesc: finishDesc.value })
    ElMessage.success('已完工'); finishVisible.value = false; reload()
  } finally { saving.value = false }
}
async function doClose(row) {
  const { value } = await ElMessageBox.prompt('请输入关闭原因（选填）', '关闭工单', {
    type: 'warning', inputPlaceholder: '选填', inputValue: ''
  }).catch(() => ({ value: null }))
  if (value === null) return
  await request.put('/repair/close/' + row.id, { remark: value })
  ElMessage.success('已关闭'); reload()
}
async function openDetail(row) {
  const res = await request.get('/repair/' + row.id)
  detail.value = res.data
  rateValue.value = res.data.rating || 5
  detailVisible.value = true
}
async function doRate() {
  await request.put('/repair/rate/' + detail.value.id, { rating: rateValue.value })
  ElMessage.success('评价成功'); detailVisible.value = false; reload()
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除工单 ' + row.orderNo + '？', '提示', { type: 'warning' })
  await request.delete('/repair/' + row.id); ElMessage.success('已删除'); reload()
}

onMounted(() => { load(); loadStats(); loadDict(); loadHandlers() })
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card { padding: 14px 16px; border-radius: 10px; cursor: pointer; border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color); transition: all .18s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,.08); }
.stat-num { font-size: 22px; font-weight: 700; color: var(--el-color-primary); }
.stat-label { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.stat-card.is-pending .stat-num { color: var(--el-color-info); }
.stat-card.is-assigned .stat-num { color: var(--el-color-warning); }
.stat-card.is-processing .stat-num { color: var(--el-color-primary); }
.stat-card.is-finished .stat-num { color: var(--el-color-success); }
.stat-card.is-closed .stat-num { color: var(--el-text-color-secondary); }
.rate-box { display: flex; align-items: center; gap: 12px; margin: 16px 0; flex-wrap: wrap; }
.log-title { margin: 20px 0 12px; font-weight: 600; }
.log-content { font-size: 13px; color: var(--el-text-color-regular); margin-top: 2px; }
.upload-plus { font-size: 26px; color: var(--el-text-color-placeholder); line-height: 1; }
.tip { font-size: 12px; line-height: 1.6; }
.img-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.img-thumb { width: 84px; height: 84px; border-radius: 6px; border: 1px solid var(--el-border-color-lighter); cursor: zoom-in; }
</style>