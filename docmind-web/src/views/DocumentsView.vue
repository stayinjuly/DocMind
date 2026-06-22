<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { documentApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Refresh, Delete } from '@element-plus/icons-vue'
import type { Document } from '../api/types'

const documents = ref<Document[]>([])
const loading = ref(false)
const uploadLoading = ref(false)
const uploadPublic = ref(false)
const dragActive = ref(false)

// 分页状态（el-pagination 用 1-based，后端用 0-based）
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

onMounted(() => {
  loadDocuments()
})

async function loadDocuments() {
  loading.value = true
  try {
    const response = await documentApi.list(currentPage.value - 1, pageSize.value)
    documents.value = response.data.content
    total.value = response.data.totalElements
  } catch {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number) {
  currentPage.value = page
  loadDocuments()
}

function onSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadDocuments()
}

async function handleUpload(file: File) {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!['txt', 'md', 'pdf', 'docx', 'doc', 'xlsx', 'xls', 'csv'].includes(ext || '')) {
    ElMessage.error('仅支持 TXT、Markdown、PDF、Word 和 Excel 文件')
    return
  }

  uploadLoading.value = true
  try {
    await documentApi.upload(file, uploadPublic.value)
    ElMessage.success('文档上传成功，正在后台处理中...')
    currentPage.value = 1
    loadDocuments()
  } catch (error: any) {
    const msg = error.response?.data?.message || '上传失败'
    ElMessage.error(msg)
  } finally {
    uploadLoading.value = false
  }
}

function onFileChange(uploadFile: any) {
  if (uploadFile?.raw) {
    handleUpload(uploadFile.raw)
  }
}

function onDrop(e: DragEvent) {
  dragActive.value = false
  const file = e.dataTransfer?.files[0]
  if (file) {
    handleUpload(file)
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
    // 删除当前页最后一条时回退一页，避免停留在空页
    if (documents.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
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

function fileTypeIcon(type: string): string {
  const icons: Record<string, string> = {
    pdf: '📄', docx: '📝', doc: '📝', xlsx: '📊', xls: '📊',
    csv: '📊', txt: '📃', md: '📃',
  }
  return icons[type?.toLowerCase()] || '📎'
}

function statusType(status: string) {
  const map: Record<string, string> = {
    COMPLETED: 'success',
    PROCESSING: 'warning',
    PENDING: 'info',
    FAILED: 'danger',
  }
  return map[status] || ''
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    COMPLETED: '已完成',
    PROCESSING: '处理中',
    PENDING: '待处理',
    FAILED: '失败',
  }
  return map[status] || status
}
</script>

<template>
  <div class="documents-view">
    <div class="page-header">
      <h2>文档管理</h2>
      <div class="header-actions">
        <el-button text :icon="Refresh" @click="loadDocuments" :loading="loading">
          刷新
        </el-button>
        <div class="public-toggle">
          <span class="toggle-label">公共文档</span>
          <el-switch v-model="uploadPublic" />
        </div>
        <el-upload
          :show-file-list="false"
          :before-upload="() => false"
          :on-change="onFileChange"
          accept=".txt,.md,.pdf,.docx,.doc,.xlsx,.xls,.csv"
        >
          <el-button type="primary" :icon="Upload" :loading="uploadLoading">
            上传文档
          </el-button>
        </el-upload>
      </div>
    </div>

    <div
      :class="['drop-zone', { active: dragActive }]"
      @dragover.prevent="dragActive = true"
      @dragleave.prevent="dragActive = false"
      @drop.prevent="onDrop"
    >
      <div class="drop-zone-content">
        <el-icon size="28" color="var(--color-text-tertiary)"><Upload /></el-icon>
        <span>拖拽文件到此处上传</span>
        <span class="drop-hint">支持 TXT、Markdown、PDF、Word、Excel 文件</span>
      </div>
    </div>

    <el-table
      :data="documents"
      v-loading="loading"
      class="doc-table"
      :empty-text="'暂无文档，请上传'"
    >
      <el-table-column prop="name" label="文件名" min-width="240">
        <template #default="{ row }">
          <div class="file-name-cell">
            <span class="file-icon">{{ fileTypeIcon(row.type) }}</span>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.type?.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="size" label="大小" width="100">
        <template #default="{ row }">
          {{ formatSize(row.size) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploadTime" label="上传时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.uploadTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" text :icon="Delete" @click="handleDelete(row)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.documents-view {
  padding: var(--spacing-lg);
  max-width: 1200px;
  margin: 0 auto;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-lg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.page-header h2 {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.public-toggle {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.toggle-label {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.drop-zone {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-xl);
  text-align: center;
  margin-bottom: var(--spacing-lg);
  transition: all var(--transition-fast);
  cursor: pointer;
}

.drop-zone.active {
  border-color: var(--color-primary);
  background: rgba(108, 92, 231, 0.04);
}

.drop-zone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}

.drop-hint {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.doc-table {
  border-radius: var(--radius-md);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.file-icon {
  font-size: var(--text-lg);
  flex-shrink: 0;
}
</style>
