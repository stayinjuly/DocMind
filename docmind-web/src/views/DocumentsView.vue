<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { documentApi } from '../api'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Document, UploadResponse } from '../api/types'

const userStore = useUserStore()
const documents = ref<Document[]>([])
const loading = ref(false)
const uploadLoading = ref(false)

onMounted(() => {
  loadDocuments()
})

async function loadDocuments() {
  loading.value = true
  try {
    const response = await documentApi.list()
    documents.value = response.data
  } catch {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

async function handleUpload(options: { raw: File }) {
  const file = options.raw
  const ext = file.name.split('.').pop()?.toLowerCase()

  if (!['txt', 'md'].includes(ext || '')) {
    ElMessage.error('仅支持 TXT 和 Markdown 文件')
    return
  }

  uploadLoading.value = true
  try {
    const response = await documentApi.upload(file, userStore.userId)
    const result = response.data as UploadResponse

    if (result.success) {
      ElMessage.success('文档上传成功')
      loadDocuments()
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploadLoading.value = false
  }
}

async function handleDelete(doc: Document) {
  try {
    await ElMessageBox.confirm(`确定要删除文档「${doc.name}」吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await documentApi.delete(doc.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch {
    ElMessage.error('删除失败')
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="documents-view">
    <div class="page-header">
      <h2>文档管理</h2>
      <el-upload
        :show-file-list="false"
        :before-upload="() => false"
        :on-change="handleUpload"
        accept=".txt,.md"
      >
        <el-button type="primary" :loading="uploadLoading">
          上传文档
        </el-button>
      </el-upload>
    </div>

    <el-table :data="documents" v-loading="loading" stripe>
      <el-table-column prop="name" label="文件名" min-width="200" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="size" label="大小" width="100">
        <template #default="{ row }">
          {{ formatSize(row.size) }}
        </template>
      </el-table-column>
      <el-table-column prop="uploadTime" label="上传时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.uploadTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="上传者" width="120" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" text @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && documents.length === 0" description="暂无文档，请上传" />
  </div>
</template>

<style scoped>
.documents-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}
</style>
