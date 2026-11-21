<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('job:create')" type="success" @click="openCreate">新增任务</el-button>
      <el-button v-if="store.hasPerm('job:log-list')" @click="openLogs">执行日志</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="jobName" label="任务名称" width="160" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="kindTag(row.jobKind)" size="small">{{ kindLabel(row.jobKind) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调用目标 / 脚本" min-width="200">
          <template #default="{ row }">
            <code v-if="row.jobKind === 'bean'">{{ row.invokeTarget }}</code>
            <span v-else class="script-cell" :title="row.scriptBody">{{ brief(row.scriptBody) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpr" label="Cron" width="140" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.isEnabled" @change="v => toggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.hasPerm('job:run')" link type="primary" size="small" @click="run(row)">执行</el-button>
            <el-button v-if="store.hasPerm('job:update')" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="store.hasPerm('job:delete')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="pageNum" :page-size="10" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑任务' : '新增任务'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="任务名称"><el-input v-model="form.jobName" /></el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="form.jobKind">
            <el-radio label="bean">Java 方法</el-radio>
            <el-radio v-if="canScript" label="shell">Shell 脚本</el-radio>
            <el-radio v-if="canScript" label="java">Java 代码</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.jobKind === 'bean'" label="调用目标">
          <el-select v-model="form.invokeTarget" filterable allow-create style="width:100%">
            <el-option v-for="t in targets" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="脚本内容">
          <el-input v-model="form.scriptBody" type="textarea" :rows="10" class="script-editor"
                    :placeholder="placeholderText" />
          <div class="form-tip" v-if="form.jobKind === 'java'">
            实现 execute(ScriptContext ctx) 方法；可用 ctx.getBean(...) 调用业务服务。保存时自动编译校验。
          </div>
        </el-form-item>
        <el-form-item label="Cron 表达式">
          <el-input v-model="form.cronExpr" placeholder="0 0 2 * * ? （6位：秒 分 时 日 月 周）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isEnabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logVisible" title="执行日志" width="900px">
      <div class="toolbar">
        <el-button type="danger" size="small" @click="cleanLogs">清空日志</el-button>
      </div>
      <el-table :data="logs" border stripe size="small" max-height="500">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="jobName" label="任务" width="140" />
        <el-table-column prop="jobKind" label="类型" width="80" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.isSuccess ? 'success' : 'danger'" size="small">{{ row.isSuccess ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时ms" width="90" />
        <el-table-column prop="firedAt" label="时间" width="160" />
        <el-table-column prop="logMessage" label="信息" min-width="240" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const canScript = computed(() => store.hasPerm('job:script'))
const placeholderText = computed(() => form.jobKind === 'shell'
  ? '输入 Shell 命令，如 echo hello'
  : 'public Object execute(ScriptContext ctx) { return "ok"; }')
const loading = ref(false), saving = ref(false)
const list = ref([]), total = ref(0), pageNum = ref(1)
const dialogVisible = ref(false), editId = ref(null)
const logVisible = ref(false), logs = ref([]), targets = ref([])
const form = reactive({ jobName: '', jobGroup: 'DEFAULT', jobKind: 'bean', invokeTarget: '', scriptBody: '', cronExpr: '0 0 2 * * ?', allowConcurrent: false, isEnabled: true, description: '' })

function kindLabel(k) { return { bean: 'Java方法', shell: 'Shell脚本', java: 'Java代码' }[k] || k }
function kindTag(k) { return { bean: 'info', shell: 'warning', java: 'success' }[k] || 'info' }
function brief(s) { if (!s) return '-'; const o = s.replace(/\s+/g, ' ').trim(); return o.length > 50 ? o.slice(0, 50) + '...' : o }

async function load() {
  loading.value = true
  try { const res = await request.get('/job/list', { params: { pageNum: pageNum.value, pageSize: 10 } }); list.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
function resetForm() {
  Object.assign(form, { jobName: '', jobGroup: 'DEFAULT', jobKind: 'bean', invokeTarget: '', scriptBody: '', cronExpr: '0 0 2 * * ?', allowConcurrent: false, isEnabled: true, description: '' })
  editId.value = null
}
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row) { resetForm(); Object.assign(form, row); editId.value = row.id; dialogVisible.value = true }
async function submit() {
  saving.value = true
  try {
    const payload = { ...form, id: editId.value || undefined }
    if (editId.value) await request.put('/job', payload)
    else await request.post('/job', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; load()
  } finally { saving.value = false }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除任务 ' + row.jobName + '？', '提示', { type: 'warning' })
  await request.delete('/job/' + row.id); ElMessage.success('已删除'); load()
}
async function toggle(row, val) {
  await request.put('/job/toggle/' + row.id, { enabled: val })
  ElMessage.success(val ? '已启用' : '已停用'); load()
}
async function run(row) {
  const res = await request.post('/job/run/' + row.id)
  ElMessage.success('执行结果: ' + (res.data || '完成'))
}
async function openLogs() {
  const res = await request.get('/job/log/list', { params: { pageNum: 1, pageSize: 100 } })
  logs.value = res.data.records; logVisible.value = true
}
async function cleanLogs() {
  await ElMessageBox.confirm('确认清空全部执行日志？', '提示', { type: 'warning' })
  await request.delete('/job/log/clean'); ElMessage.success('已清空'); openLogs()
}
onMounted(async () => { load(); const r = await request.get('/job/targets'); targets.value = r.data || [] })
</script>

<style scoped>
.script-cell { font-family: monospace; font-size: 12px; color: #606266; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 4px; }
</style>
