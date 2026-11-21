<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button v-if="store.hasPerm('login-log:clean')" type="danger" @click="clean">清空记录</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="ipAddr" label="IP" width="150" />
        <el-table-column prop="osName" label="系统" width="110" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.isSuccess ? 'success' : 'danger'" size="small">{{ row.isSuccess ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="message" label="说明" width="160" />
        <el-table-column prop="loginAt" label="时间" width="170" />
        <el-table-column prop="userAgent" label="浏览器标识" min-width="240" show-overflow-tooltip />
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
  try { const res = await request.get('/login-log/list', { params: { pageNum: pageNum.value, pageSize: 10 } }); list.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
async function clean() {
  await ElMessageBox.confirm('确认清空全部登录记录？', '提示', { type: 'warning' })
  await request.delete('/login-log/clean').catch(() => {})
  ElMessage.success('已清空'); load()
}
onMounted(load)
</script>
