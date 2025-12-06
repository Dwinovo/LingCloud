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
        <div
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item"
          :class="{ active: activeMenu === item.key }"
          @click="handleMenuSelect(item.key)"
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>

      <!-- 文件管理区域 -->
      <div class="file-manager">
        <template v-if="activeMenu === 'files'">
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
              table-layout="fixed"
              empty-text="暂无文件"
            >
              <el-table-column prop="name" label="文件名" min-width="200" align="center">
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
              <el-table-column prop="size" label="大小" width="120" align="center">
                <template #default="{ row }">
                  {{ formatFileSize(row.size) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="180" align="center">
                <template #default="{ row }">
                  {{ formatDate(row.uploadTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
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
        </template>

        
        <div v-else class="placeholder-section">
          <el-empty :description="placeholderMessages[activeMenu] || '功能开发中，敬请期待'" />
        </div>
      </div>
    </div>
  </div>

  <el-dialog
    v-model="powDialogVisible"
    title="PoW 验证"
    width="420px"
    :close-on-click-modal="false"
    @close="cancelPowUpload"
  >
    <div class="dialog-content">
      <p class="pow-tip">{{ powDialogMessage }}</p>
    </div>
    <template #footer>
      <el-button @click="cancelPowUpload">取消</el-button>
      <el-button type="primary" @click="confirmPowUpload">继续上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadInstance, type UploadUserFile } from 'element-plus'
import {
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
import { authApi, fileApi, type InitUploadResponse, type UserFileInfo, type DownloadFileInfo } from '../utils/api'
import { convergentEncrypt, buildUploadMessage, calculateFileHash, convergentDecrypt, downloadFile } from '../utils/crypto'

const router = useRouter()
const authStore = useAuthStore()

const uploadRef = ref<UploadInstance>()
const uploading = ref(false)

const powDialogVisible = ref(false)
const powDialogMessage = ref('')
const powDialogStatus = ref<number | null>(null)
const powDialogIv = ref<string | null>(null)
const pendingPowFile = ref<UploadUserFile | null>(null)
const pendingPowHashBase64 = ref('')
const pendingPowHashHex = ref('')
const powPromiseResolve = ref<(() => void) | null>(null)
const powPromiseReject = ref<((reason?: any) => void) | null>(null)
const fileListLoading = ref(false)
const uploadFiles = ref<UploadUserFile[]>([])
type MenuKey = 'files' | 'share' | 'settings'
interface MenuItem {
  key: MenuKey
  label: string
  icon: Component
}
const menuItems: MenuItem[] = [
  { key: 'files', label: '我的文件', icon: Folder },
  { key: 'share', label: '分享文件', icon: Share },
  { key: 'settings', label: '设置', icon: Setting }
]
const placeholderMessages: Record<MenuKey, string> = {
  files: '',
  share: '分享文件功能开发中，敬请期待',
  settings: '设置功能开发中，敬请期待'
}
const activeMenu = ref<MenuKey>('files')

interface FileItem {
  id: string
  name: string
  size: number
  type: string
  uploadTime: Date
  hashPlain: string
}

const fileList = ref<FileItem[]>([])

const handleMenuSelect = (key: MenuKey) => {
  activeMenu.value = key
}

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
  }).then(async () => {
    try {
      // 调用后端logout接口，让后端清除Cookie
      await authApi.logout()
      // 清除前端用户状态
      authStore.clearUserInfo()
      // 跳转到登录页面
      window.location.href = '/login'
      ElMessage.success('已退出登录')
    } catch (error: any) {
      console.error('退出登录失败:', error)
      ElMessage.error('退出登录失败：' + (error.response?.data?.message || error.message))
    }
  })
}

// 处理文件选择
const handleFileChange = (_file: any, fileList: any[]) => {
  uploadFiles.value = fileList
}

// 处理文件移除
const handleFileRemove = (_file: any, fileList: any[]) => {
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
    for (const fileObj of uploadFiles.value) {
      if (!fileObj.raw) {
        continue
      }
      await processSingleFile(fileObj)
    }
    ElMessage.success('所有文件上传成功！')
    clearUploadList()
    refreshFileList()
  } catch (error: any) {
    ElMessage.error('上传失败：' + (error.response?.data?.message || error.message))
  } finally {
    uploading.value = false
  }
}

const processSingleFile = async (fileObj: UploadUserFile) => {
  ElMessage.info(`正在初始化 ${fileObj.name}...`)
  const hashHex = await calculateFileHash(fileObj.raw!)
  const hashBase64 = hexToBase64(hashHex)
  const initResponse = await fileApi.init(hashBase64)
  const initData = initResponse.data.data

  if (!initData) {
    throw new Error('初始化响应异常')
  }

  await handleInitResult(fileObj, { base64: hashBase64, hex: hashHex }, initData)
}

const handleInitResult = async (
  fileObj: UploadUserFile,
  hashInfo: { base64: string; hex: string },
  initData: InitUploadResponse
) => {
  if (initData.status === 2) { // ALLOW
    await ElMessageBox.alert(`${fileObj.name} 已存在，秒传成功`, '提示', { confirmButtonText: '知道了' })
    return fileObj.name
  }

  if (initData.status === 0 || initData.status === 1) {
    powDialogStatus.value = initData.status
    powDialogMessage.value = initData.message
    powDialogIv.value = initData.status === 0 ? initData.iv ?? null : null
    pendingPowFile.value = fileObj
    pendingPowHashBase64.value = hashInfo.base64
    pendingPowHashHex.value = hashInfo.hex
    console.log('[POW][Init]', {
      file: fileObj.name,
      status: initData.status,
      message: initData.message,
      hashBase64: hashInfo.base64.slice(0, 16) + '...',
      ivProvided: !!powDialogIv.value
    })
    await new Promise<void>((resolve, reject) => {
      powPromiseResolve.value = resolve
      powPromiseReject.value = reject
      powDialogVisible.value = true
    })
    return
  }

  ElMessage.warning(`${fileObj.name} 返回未知状态：${initData.status}，已跳过`)
}

const confirmPowUpload = async () => {
  if (!pendingPowFile.value || !pendingPowFile.value.raw) {
    powDialogVisible.value = false
    powPromiseReject.value?.(new Error('缺少POW文件'))
    powPromiseResolve.value = null
    powPromiseReject.value = null
    return
  }

  try {
    ElMessage.info(`正在加密 ${pendingPowFile.value.name}...`)
    const fixedIv = powDialogStatus.value === 0 ? powDialogIv.value : null
    if (powDialogStatus.value === 0 && !fixedIv) {
      throw new Error('后端未返回IV，无法验证POW')
    }
    const encryptedData = await convergentEncrypt(pendingPowFile.value.raw, pendingPowHashHex.value, fixedIv || undefined)
    const uploadMessage = buildUploadMessage(encryptedData)
    console.log('[POW][Encrypt]', {
      file: pendingPowFile.value.name,
      iv: uploadMessage.IV,
      tag: uploadMessage.TAG,
      hashPreview: uploadMessage.H.slice(0, 16) + '...',
      cipherSize: uploadMessage.C.size
    })

    await submitPow(uploadMessage)
    ElMessage.success(`${pendingPowFile.value.name} POW 提交成功`)
    await refreshFileList()
    powPromiseResolve.value?.()
  } catch (error: any) {
    powPromiseReject.value?.(error)
    ElMessage.error('POW上传失败：' + (error.response?.data?.message || error.message))
  } finally {
    powDialogVisible.value = false
    pendingPowFile.value = null
    powDialogStatus.value = null
    pendingPowHashBase64.value = ''
    pendingPowHashHex.value = ''
    powDialogIv.value = null
    powPromiseResolve.value = null
    powPromiseReject.value = null
  }
}

const cancelPowUpload = () => {
  powDialogVisible.value = false
  powPromiseReject.value?.(new Error('用户取消POW'))
  powPromiseResolve.value = null
  powPromiseReject.value = null
  pendingPowFile.value = null
  pendingPowStatusReset()
}

const pendingPowStatusReset = () => {
  powDialogStatus.value = null
  powDialogIv.value = null
  pendingPowHashBase64.value = ''
  pendingPowHashHex.value = ''
}

const submitPow = async (uploadMessage: ReturnType<typeof buildUploadMessage>) => {
  const payloadBlob = await buildPowPayload(uploadMessage, pendingPowHashBase64.value)
  const payloadFile = new File([payloadBlob], `${pendingPowFile.value?.name || 'payload'}.bin`, {
    type: 'application/octet-stream'
  })
  console.log('[POW][Payload]', {
    file: payloadFile.name,
    totalBytes: payloadBlob.size,
    sections: {
      iv: 12,
      hash: 32,
      tag: 16,
      cipher: payloadBlob.size - 60
    }
  })
  const formData = new FormData()
  formData.append('payload', payloadFile)
  formData.append('filename', pendingPowFile.value?.name || payloadFile.name)
  await fileApi.submitPow(formData)
}

const buildPowPayload = async (uploadMessage: ReturnType<typeof buildUploadMessage>, hashBase64: string): Promise<Blob> => {
  if (!hashBase64) {
    throw new Error('缺少文件哈希，无法生成POW payload')
  }
  const ivBytes = base64ToUint8Array(uploadMessage.IV)
  const hashBytes = base64ToUint8Array(hashBase64)
  const tagBytes = base64ToUint8Array(uploadMessage.TAG)
  const cipherBuffer = await uploadMessage.C.arrayBuffer()
  const cipherBytes = new Uint8Array(cipherBuffer)

  const totalLength = ivBytes.length + hashBytes.length + tagBytes.length + cipherBytes.length
  const payload = new Uint8Array(totalLength)
  payload.set(ivBytes, 0)
  payload.set(hashBytes, ivBytes.length)
  payload.set(tagBytes, ivBytes.length + hashBytes.length)
  payload.set(cipherBytes, ivBytes.length + hashBytes.length + tagBytes.length)

  return new Blob([payload], { type: 'application/octet-stream' })
}

const base64ToUint8Array = (base64: string): Uint8Array => {
  const binaryString = atob(base64)
  const len = binaryString.length
  const bytes = new Uint8Array(len)
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  return bytes
}

const base64ToHex = (base64: string): string => {
  const bytes = base64ToUint8Array(base64)
  return Array.from(bytes)
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

const hexToBase64 = (hex: string): string => {
  const bytes = hexToUint8Array(hex)
  let binary = ''
  bytes.forEach(byte => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary)
}

const hexToUint8Array = (hex: string): Uint8Array => {
  const cleanHex = hex.startsWith('0x') ? hex.slice(2) : hex
  const bytes = new Uint8Array(cleanHex.length / 2)
  for (let i = 0; i < cleanHex.length; i += 2) {
    bytes[i / 2] = parseInt(cleanHex.substr(i, 2), 16)
  }
  return bytes
}


// 刷新文件列表
const refreshFileList = async () => {
  fileListLoading.value = true
  try {
    const response = await fileApi.listFiles()
    const files: UserFileInfo[] = response.data.data || []
    fileList.value = files.map((item) => {
      const uploadDate = item.updatedAt ? new Date(item.updatedAt) : item.createdAt ? new Date(item.createdAt) : new Date()
      return {
        id: item.id,
        name: item.name || '未命名文件',
        size: 0,
        type: inferFileType(item.name || ''),
        uploadTime: uploadDate,
        hashPlain: base64ToHex(item.fileHash)
      }
    })
  } catch (error: any) {
    ElMessage.error('获取文件列表失败：' + (error.response?.data?.message || error.message))
  } finally {
    fileListLoading.value = false
  }
}

// 下载文件
const handleDownload = async (file: FileItem) => {
  try {
    ElMessage.info(`正在准备下载：${file.name}`)
    const response = await fileApi.download(file.id)
    const meta: DownloadFileInfo | undefined = response.data.data
    if (!meta) {
      throw new Error('下载元数据为空')
    }

    const encryptedResp = await fetch(meta.url)
    if (!encryptedResp.ok) {
      throw new Error(`下载密文失败(${encryptedResp.status})`)
    }
    const encryptedBlob = await encryptedResp.blob()

    const decryptedBlob = await convergentDecrypt(file.hashPlain, meta.iv, meta.tag, encryptedBlob)
    const filename = file.name || 'download.bin'
    downloadFile(decryptedBlob, filename)
    ElMessage.success(`${filename} 下载完成`)
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
      await fileApi.delete(file.id)
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
const formatDate = (date: Date | string): string => {
  return new Date(date).toLocaleString('zh-CN')
}

const inferFileType = (filename: string): string => {
  const ext = filename.split('.').pop()?.toLowerCase()
  if (!ext) return 'document'
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp', 'svg', 'webp'].includes(ext)) return 'image'
  if (['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) return 'video'
  if (['mp3', 'wav', 'flac', 'aac', 'ogg'].includes(ext)) return 'audio'
  if (['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'md'].includes(ext)) return 'document'
  return 'document'
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

.primary-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 26px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 0.025em;
  height: 52px;
}

.primary-upload-btn:hover {
  background: linear-gradient(135deg, #0284c7, #0c4a6e);
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.4);
}

.primary-upload-btn:active {
  transform: translateY(0);
}

.file-list {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}



.placeholder-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
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



/* 简化表格内部结构，减少冗余嵌套 */
:deep(.el-table__body-wrapper) {
  position: relative;
  overflow: hidden;
}

:deep(.el-table__body-wrapper .el-scrollbar) {
  height: 100%;
}

:deep(.el-table__body-wrapper .el-scrollbar__wrap) {
  overflow-x: auto;
  overflow-y: hidden;
}

:deep(.el-table__body-wrapper .el-scrollbar__view) {
  width: 100%;
}

:deep(.el-dialog__body) {
  color: #1e293b;
  font-size: 15px;
  padding-top: 0;
  text-align: left;
}

.dialog-content {
  text-align: left;
  margin: 0;
}

.pow-tip {
  margin: 8px 0 0;
  line-height: 1.5;
  color: #475569;
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
