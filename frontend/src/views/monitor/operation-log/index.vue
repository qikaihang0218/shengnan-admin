<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('operation-log:clean')" type="danger" @click="clean">清空日志</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="moduleName" label="模块" width="120" />
        <el-table-column prop="actionDesc" label="操作" width="150" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="requestUri" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="costMs" label="耗时ms" width="90" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.isSuccess ? 'success' : 'danger'" size="small">{{ row.isSuccess ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="operatedAt" label="时间" width="170" />
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="pageNum" :page-size="10" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), list = ref([]), total = ref(0), pageNum = ref(1)
async function load() {
  loading.value = true
  try { const res = await request.get('/operation-log/list', { params: { pageNum: pageNum.value, pageSize: 10 } }); list.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
async function clean() {
  await ElMessageBox.confirm('确认清空全部操作日志？', '提示', { type: 'warning' })
  await request.delete('/operation-log/clean').catch(() => {})
  ElMessage.success('已清空'); load()
}
onMounted(load)
</script>
