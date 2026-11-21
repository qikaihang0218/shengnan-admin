<template>
  <div class="page-card">
    <div class="toolbar">
      <el-upload :action="uploadUrl" :with-credentials="true" :show-file-list="false"
                 :on-success="onUpload" :on-error="onError" multiple>
        <el-button v-if="store.hasPerm('file:upload')" type="success">上传文件</el-button>
      </el-upload>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="originalName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column prop="contentType" label="类型" width="180" show-overflow-tooltip />
        <el-table-column prop="uploader" label="上传者" width="120" />
        <el-table-column prop="uploadedAt" label="上传时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isImage(row)" link type="info" size="small" @click="preview(row)">预览</el-button>
            <el-button v-if="store.hasPerm('file:download')" link type="primary" size="small" @click="download(row)">下载</el-button>
            <el-button v-if="store.hasPerm('file:delete')" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="previewVisible" :title="previewName" width="800px">
      <img v-if="previewUrl" :src="previewUrl" style="max-width:100%;max-height:70vh;display:block;margin:0 auto" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { useUserStore } from '../../../store/user'

const store = useUserStore()
const loading = ref(false), list = ref([])
const previewVisible = ref(false), previewUrl = ref(''), previewName = ref('')
const uploadUrl = '/api/file/upload'

async function load() {
  loading.value = true
  try { const res = await request.get('/file/list'); list.value = res.data || [] }
  finally { loading.value = false }
}
function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, n = bytes
  while (n >= 1024 && i < units.length - 1) { n /= 1024; i++ }
  return n.toFixed(1) + ' ' + units[i]
}
function isImage(row) { return (row.contentType || '').startsWith('image/') }

async function preview(row) {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  const res = await fetch('/api/file/preview/' + row.id, { credentials: 'include' })
  const blob = await res.blob()
  previewUrl.value = URL.createObjectURL(blob)
  previewName.value = row.originalName
  previewVisible.value = true
}
async function download(row) {
  const res = await fetch('/api/file/download/' + row.id, { credentials: 'include' })
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = row.originalName
  a.click()
  URL.revokeObjectURL(url)
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除文件 ' + row.originalName + '？', '提示', { type: 'warning' })
  await request.delete('/file/' + row.id); ElMessage.success('已删除'); load()
}
function onUpload() { ElMessage.success('上传成功'); load() }
function onError() { ElMessage.error('上传失败') }
function cleanup() { if (previewUrl.value) URL.revokeObjectURL(previewUrl.value) }
onMounted(load)
onBeforeUnmount(cleanup)
</script>
