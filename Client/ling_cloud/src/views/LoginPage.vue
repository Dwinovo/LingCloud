<template>
  <div class="background">
    <div class="login-box">
      <div class="login-title">
        <div class="logo">
          <h1>LingCloud</h1>
        </div>
        <p class="subtitle">安全加密的云存储服务</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="账号" size="large" prefix-icon="User" clearable/>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password clearable @keyup.enter="handleLogin"/>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="login-form"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名"
                size="large"
                prefix-icon="User"
                clearable
              />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input
                v-model="registerForm.nickname"
                placeholder="昵称"
                size="large"
                prefix-icon="Avatar"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码"
                size="large"
                prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                prefix-icon="Lock"
                show-password
                clearable
                @keyup.enter="handleRegister"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="registerLoading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '../utils/api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loginLoading.value = true

    const response = await authApi.login(loginForm.username, loginForm.password)

    console.log('登录响应:', response.data)

    if (response.data.code === 0) {
      authStore.setUserInfo(response.data.data)
      ElMessage.success('登录成功')

      // 使用Vue Router编程式导航 - 官方推荐方式
      console.log('登录成功，准备跳转到dashboard')
      console.log('Cookie状态:', document.cookie)

      // 立即执行导航，不需要延迟
      router.push({ name: 'dashboard' })
        .then(() => {
          console.log('导航成功完成')
        })
        .catch((error) => {
          console.error('导航失败:', error)
        })
    } else {
      ElMessage.error(response.data.message || '登录失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    await registerFormRef.value.validate()
    registerLoading.value = true

    const response = await authApi.register(
      registerForm.username,
      registerForm.password,
      registerForm.nickname
    )

    if (response.data.code === 0) {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      // 清空注册表单
      Object.assign(registerForm, {
        username: '',
        nickname: '',
        password: '',
        confirmPassword: ''
      })
    } else {
      ElMessage.error(response.data.message || '注册失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '注册失败')
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.background {
  width: 100%;
  height: 100vh;
  background: #e0f2fe;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
  margin: 0;
  position: fixed;
  top: 0;
  left: 0;
}

.login-box {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 460px;
  padding: 48px 40px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 0;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.logo h1 {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: -0.025em;
}

.logo i {
  font-size: 40px;
  color: #0ea5e9;
}

.subtitle {
  color: #64748b;
  font-size: 16px;
  margin: 0;
  font-weight: 400;
}

.login-tabs {
  margin-top: 20px;
}

:deep(.el-tabs__header) {
  margin: 0 0 0 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
  padding: 0 24px 16px 24px;
  transition: all 0.3s ease;
}

:deep(.el-tabs__item:hover) {
  color: #0ea5e9;
}

:deep(.el-tabs__item.is-active) {
  color: #0ea5e9;
}

:deep(.el-tabs__active-bar) {
  background-color: #0ea5e9;
  height: 3px;
  border-radius: 3px;
}

.login-form {
  width: 100%;
  margin-top: 24px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
  width: 90%;
  margin-left: auto;
  margin-right: auto;
  display: flex;
  justify-content: center;
}

:deep(.el-input__wrapper) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e2e8f0;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
  transition: all 0.3s ease;
  letter-spacing: 0.025em;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #0284c7, #0c4a6e);
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

@media (max-width: 480px) {
  .login-container {
    padding: 10px;
  }

  .login-box {
    padding: 30px 20px;
  }

  .logo h1 {
    font-size: 28px;
  }

  .logo i {
    font-size: 32px;
  }
}
</style>
