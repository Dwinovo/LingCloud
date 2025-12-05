<template>
  <div class="dashboard-container">
    <!-- 顶部导航栏 -->
    <div class="navbar">
      <div class="navbar-left">
        <div class="logo">
          <span>LingCloud</span>
        </div>
      </div>
      <div class="navbar-right">
        <el-dropdown @command="handleUserMenuCommand">
          <div class="user-info">
            <span class="username">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 侧边栏 -->
      <div class="sidebar">
        <div class="menu-item active">
          <el-icon><Folder /></el-icon>
          <span>我的文件</span>
        </div>
        <div class="menu-item">
          <el-icon><Upload /></el-icon>
          <span>上传记录</span>
        </div>
        <div class="menu-item">
          <el-icon><Share /></el-icon>
          <span>分享文件</span>
        </div>
        <div class="menu-item">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </div>
      </div>

      <!-- 文件管理区域 -->
      <div class="file-manager">
        <!-- 文件上传区域 -->
        <div class="upload-section">
          <el-upload
            ref="uploadRef"
            class="upload-dragger"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="uploadFiles"
            multiple
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖拽到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持多文件上传，文件会自动加密并去重
              </div>
            </template>
          </el-upload>
          <div v-if="uploadFiles.length > 0" class="upload-actions">
            <el-button
              type="primary"
              :loading="uploading"
              @click="handleUpload"
            >
              <el-icon><Upload /></el-icon>
              开始上传
            </el-button>
            <el-button @click="clearUploadList">
              <el-icon><Delete /></el-icon>
              清空列表
            </el-button>
          </div>
        </div>

        <!-- 文件列表 -->
        <div class="file-list">
          <div class="list-header">
            <h3>文件列表</h3>
            <div class="list-actions">
              <el-button size="small" @click="refreshFileList">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="fileListLoading"
            :data="fileList"
            style="width: 100%"
            empty-text="暂无文件"
          >
            <el-table-column prop="name" label="文件名" min-width="200">
              <template #default="{ row }">
                <div class="file-name">
                  <el-icon class="file-icon">
                    <Document v-if="row.type === 'document'" />
                    <Picture v-else-if="row.type === 'image'" />
                    <VideoPlay v-else-if="row.type === 'video'" />
                    <VideoCamera v-else-if="row.type === 'audio'" />
                    <Files v-else />
                  </el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="size" label="大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.size) }}
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" label="上传时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.uploadTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="primary"
                  @click="handleDownload(row)"
                >
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click="handleDelete(row)"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadInstance, type UploadUserFile } from 'element-plus'
import {
  UserFilled,
  ArrowDown,
  Folder,
  Upload,
  Share,
  Setting,
  UploadFilled,
  Delete,
  Refresh,
  Document,
  Picture,
  VideoPlay,
  VideoCamera,
  Files,
  Download
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { fileApi } from '../utils/api'
import { calculateFileHash, convergentEncrypt } from '../utils/crypto'

const router = useRouter()
const authStore = useAuthStore()

const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const fileListLoading = ref(false)
const uploadFiles = ref<UploadUserFile[]>([])

interface FileItem {
  id: string
  name: string
  size: number
  type: string
  uploadTime: Date
  hashPlain: string
}

const fileList = ref<FileItem[]>([])

// 处理用户菜单命令
const handleUserMenuCommand = (command: string) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人资料功能开发中')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.clearUserInfo()
    router.push('/login')
    ElMessage.success('已退出登录')
  })
}

// 处理文件选择
const handleFileChange = (file: any, fileList: any[]) => {
  uploadFiles.value = fileList
}

// 处理文件移除
const handleFileRemove = (file: any, fileList: any[]) => {
  uploadFiles.value = fileList
}

// 清空上传列表
const clearUploadList = () => {
  uploadFiles.value = []
  uploadRef.value?.clearFiles()
}

// 文件上传
const handleUpload = async () => {
  if (uploadFiles.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true

  try {
    const uploadPromises = uploadFiles.value.map(async (fileObj) => {
      if (!fileObj.raw) return Promise.resolve()

      // 计算文件哈希
      ElMessage.info(`正在计算 ${fileObj.name} 的哈希值...`)
      const hashPlain = await calculateFileHash(fileObj.raw)

      // 加密文件
      ElMessage.info(`正在加密 ${fileObj.name}...`)
      const encryptedBlob = await convergentEncrypt(fileObj.raw, hashPlain)

      // 创建FormData
      const formData = new FormData()
      formData.append('hash_plain', hashPlain)
      formData.append('file', encryptedBlob, fileObj.name)

      // 上传文件
      await fileApi.upload(formData)
      return fileObj.name
    })

    await Promise.all(uploadPromises)
    ElMessage.success('所有文件上传成功！')
    clearUploadList()
    refreshFileList()
  } catch (error: any) {
    ElMessage.error('上传失败：' + (error.response?.data?.message || error.message))
  } finally {
    uploading.value = false
  }
}

// 刷新文件列表
const refreshFileList = async () => {
  fileListLoading.value = true
  try {
    // 模拟文件列表数据，实际应该从API获取
    await new Promise(resolve => setTimeout(resolve, 1000))
    fileList.value = [
      {
        id: '1',
        name: '示例文档.pdf',
        size: 1024 * 1024 * 2.5,
        type: 'document',
        uploadTime: new Date('2024-01-15 10:30:00'),
        hashPlain: 'abc123...'
      },
      {
        id: '2',
        name: '风景图片.jpg',
        size: 1024 * 512,
        type: 'image',
        uploadTime: new Date('2024-01-14 15:20:00'),
        hashPlain: 'def456...'
      }
    ]
  } catch (error: any) {
    ElMessage.error('获取文件列表失败：' + (error.response?.data?.message || error.message))
  } finally {
    fileListLoading.value = false
  }
}

// 下载文件
const handleDownload = async (file: FileItem) => {
  try {
    ElMessage.info(`正在下载 ${file.name}...`)

    // 实际应该调用下载API获取加密数据
    // const response = await fileApi.download(file.id)
    // const encryptedData = response.data

    // 模拟下载过程
    await new Promise(resolve => setTimeout(resolve, 2000))

    // 解密文件（这里需要获取原始的hashPlain作为密钥）
    // const decryptedBlob = await convergentDecrypt(encryptedData, file.hashPlain)
    // downloadFile(decryptedBlob, file.name)

    ElMessage.success(`${file.name} 下载完成`)
  } catch (error: any) {
    ElMessage.error('下载失败：' + (error.response?.data?.message || error.message))
  }
}

// 删除文件
const handleDelete = (file: FileItem) => {
  ElMessageBox.confirm(`确定要删除文件 "${file.name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 实际应该调用删除API
      // await fileApi.delete(file.id)
      ElMessage.success('文件删除成功')
      refreshFileList()
    } catch (error: any) {
      ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
    }
  })
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (date: Date): string => {
  return new Date(date).toLocaleString('zh-CN')
}

// 组件挂载时初始化认证状态并刷新文件列表
onMounted(async () => {
  // 初始化认证状态，如果失败则重定向到登录页
  const isAuthenticated = await authStore.initAuth()
  if (!isAuthenticated) {
    router.push('/login')
    return
  }

  refreshFileList()
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  background: #e0f2fe;
  margin: 0;
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.navbar {
  height: 70px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.navbar-left .logo {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 22px;
  font-weight: 700;
  color: #0ea5e9;
}

.navbar-left .logo i {
  font-size: 28px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
  width: 100%;
  justify-content: flex-start;
}

.user-info:hover {
  background-color: #f1f5f9;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

/* Dropdown样式 */
:deep(.el-dropdown-menu) {
  border-radius: 16px;
  width: 100%;
  min-width: 200px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-dropdown-menu__item) {
  border-radius: 8px;
  margin: 4px 8px;
  transition: all 0.2s ease;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: rgba(14, 165, 233, 0.1);
  color: #0ea5e9;
}

/* 去掉右边图标的左边距 */
:deep(.el-icon--right) {
  margin-left: 0 !important;
}

/* 移除dropdown的焦点样式 */
:deep(.el-dropdown:focus),
:deep(.el-dropdown:focus-visible),
:deep(.user-info:focus),
:deep(.user-info:focus-visible) {
  outline: none;
  box-shadow: none;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(226, 232, 240, 0.3);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 28px;
  cursor: pointer;
  color: #64748b;
  transition: all 0.3s ease;
  font-size: 15px;
  font-weight: 500;
  margin: 4px 5%;
  width: 90%;
  border-radius: 10px;
}

.menu-item:hover {
  background-color: rgba(240, 249, 255, 0.7);
  color: #0ea5e9;
  transform: translateX(4px);
}

.menu-item.active {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: white;
  transform: none;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
}

.file-manager {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

.upload-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 28px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

:deep(.upload-dragger) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 160px;
  border: 2px dashed #0ea5e9;
  border-radius: 16px;
  background: rgba(240, 249, 255, 0.5);
  transition: all 0.3s ease;
}

:deep(.el-upload-dragger:hover) {
  border-color: #0284c7;
  background: rgba(240, 249, 255, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(14, 165, 233, 0.2);
}

.upload-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.file-list {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #3b82f6;
  font-size: 16px;
}

:deep(.el-table) {
  border-radius: 8px;
}

:deep(.el-table th) {
  background-color: #f8fafc;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-table td) {
  border-bottom: 1px solid #f1f5f9;
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .file-manager {
    padding: 16px;
  }

  .upload-section,
  .file-list {
    padding: 16px;
  }
}
</style>